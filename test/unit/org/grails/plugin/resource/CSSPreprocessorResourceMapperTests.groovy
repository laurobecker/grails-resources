package org.grails.plugin.resource

import org.apache.commons.io.FileUtils

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.mock.web.MockServletContext
import groovy.util.ConfigObject

import grails.test.*

class CSSPreprocessorResourceMapperTests extends GrailsUnitTestCase {
    

    void setUp() {
        super.setUp()
        mockLogging(CSSPreprocessorResourceMapper)
        FileUtils.cleanDirectory(new File('./test-tmp/'));
        
    }
    /**
     * This simulates a test where the image resources are moved to a new flat dir
     * but the CSS is *not* moved, to force recalculation of paths
     */
    void testCSSPreprocessing() {

        def svc = [
            config : [ rewrite: [css: true] ]
        ]
        def base = new File('./test-tmp/')
        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = base
        r.actualUrl = r.sourceUrl
        r.contentType = "text/css"
        r.processedFile = new File(base, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(bg4.png) }
.bg5 { background: url(http://google.com/images/bg5.png) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)
        
        new CSSPreprocessorResourceMapper().with {
            resourceService = svc
            map(r, new ConfigObject())
        }

        def outcome = r.processedFile.text
        def expected = """
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/css/images/bg2.png) }
.bg3 { background: url(/images/bg3.png) }
.bg4 { background: url(resource:/css/bg4.png) }
.bg5 { background: url(http://google.com/images/bg5.png) }
"""
    
        assertEquals expected, outcome
    }

    /**
     * This simulates CSS that uses some MS IE css behaviour hacks that can cause problems
     * as they are not valid URLs
     */
    void testCSSPreprocessingWithInvalidURI() {
        def svc = [
            getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                new ResourceMeta(actualUrl: uri, processedFile: new File(uri+'.gz'))
            },
            config : [ rewrite: [css: true] ]
        ]

        def base = new File('./test-tmp/')

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = base
        r.actualUrl = r.sourceUrl
        r.contentType = 'text/css'
        r.processedFile = new File(base, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        new CSSPreprocessorResourceMapper().with {
            resourceService = svc
            map(r, new ConfigObject())
        }

        def outcome = r.processedFile.text
        def expected = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""

        assertEquals expected, outcome
    }
    
    /**
     * This simulates CSS that uses some MS IE css behaviour hacks that can cause problems
     * as they are not valid URLs
     */
    void testCSSPreprocessingDoesNothingToDataURLs() {
        def svc = [
            getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                new ResourceMeta(actualUrl: uri, processedFile: new File(uri+'.gz'))
            },
            config : [ rewrite: [css: true] ]
        ]

        def base = new File('./test-tmp/')

        def r = new ResourceMeta(sourceUrl:'/css/main.css')
        r.workDir = base
        r.actualUrl = r.sourceUrl
        r.contentType = 'text/css'
        r.processedFile = new File(base, 'css/main.css')
        r.processedFile.parentFile.mkdirs()
        r.processedFile.delete()

        def css = """
@font-face {
 font-family: 'BlaBlaBla';
 src: url("data:font/opentype;base64,ABCDEF123456789ABCDEF123456789") format('opentype');
}
"""
        r.processedFile << new ByteArrayInputStream(css.bytes)

        new CSSPreprocessorResourceMapper().with {
            resourceService = svc
            map(r, new ConfigObject())
        }

        def outcome = r.processedFile.text
        def expected = """
@font-face {
 font-family: 'BlaBlaBla';
 src: url("data:font/opentype;base64,ABCDEF123456789ABCDEF123456789") format('opentype');
}
"""
        assertEquals expected, outcome
    }
}