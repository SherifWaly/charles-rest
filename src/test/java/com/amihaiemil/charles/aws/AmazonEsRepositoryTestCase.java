/*
 * Copyright (c) 2016-2017, Mihai Emil Andronache
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1)Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  2)Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  3)Neither the name of charles-rest nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.amihaiemil.charles.aws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amihaiemil.charles.Link;
import com.amihaiemil.charles.SnapshotWebPage;
import com.amihaiemil.charles.WebPage;
import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import com.jcabi.http.mock.MkQuery;

/**
 * Unit tests for {@link AmazonEsRepository}
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 */
@SuppressWarnings("resource")
public class AmazonEsRepositoryTestCase {

    /**
     * IllegalStateException is thrown on missing secret key property.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void illegalStateOnMissingSecretKey() throws Exception {
        List<WebPage> pages = new ArrayList<WebPage>();
        pages.add(this.mockWebPage("http://www.test.com/crawledpage.html"));
        pages.add(this.mockWebPage("https://www.test.com/stuff/crawledpage.html"));

        System.setProperty("aws.accessKeyId", "access_key");
        System.setProperty("aws.es.region", "ro");
        System.setProperty("aws.es.endpoint", "http://localhost:8080/es");

        AmazonEsRepository repo = new AmazonEsRepository("testIndex");
        try {
            repo.export(pages);
            fail();
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Mandatory sys property aws.secretKey"));
        }
    }
    
    /**
     * IllegalStateException is thrown on missing access key property.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void illegalStateOnMissingAccessKey() throws Exception {
        List<WebPage> pages = new ArrayList<WebPage>();
        pages.add(this.mockWebPage("http://www.test.com/crawledpage.html"));
        pages.add(this.mockWebPage("https://www.test.com/stuff/crawledpage.html"));

        System.setProperty("aws.es.region", "ro");
        System.setProperty("aws.es.endpoint", "http://localhost:8080/es");

        AmazonEsRepository repo = new AmazonEsRepository("testIndex");
        try {
            repo.export(pages);
            fail();
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Mandatory sys property aws.accessKeyId"));
        }
    }
    
    /**
     * IllegalStateException is thrown on missing region property.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void illegalStateOnMissingRegion() throws Exception {
        List<WebPage> pages = new ArrayList<WebPage>();
        pages.add(this.mockWebPage("http://www.test.com/crawledpage.html"));
        pages.add(this.mockWebPage("https://www.test.com/stuff/crawledpage.html"));

        System.setProperty("aws.es.endpoint", "http://localhost:8080/es");

        AmazonEsRepository repo = new AmazonEsRepository("testIndex");
        try {
            repo.export(pages);
            fail();
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Mandatory sys property aws.es.region"));
        }
    }
    
    /**
     * A request is sent to the AWS Es bulk api service.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void sendsExportRequestToAwsEs() throws Exception {
        List<WebPage> pages = new ArrayList<WebPage>();
        pages.add(this.mockWebPage("http://www.test.com/crawledpage.html"));
        pages.add(this.mockWebPage("https://www.test.com/stuff/crawledpage.html"));

        System.setProperty("aws.accessKeyId", "access_key");
        System.setProperty("aws.secretKey", "secret_key");
        System.setProperty("aws.es.region", "ro");
        
        int port = this.port();
        System.setProperty("aws.es.endpoint", "http://localhost:" + port + "/es");
    
        
        MkContainer server = new MkGrizzlyContainer()
           .next(new MkAnswer.Simple("{\"status\":\"Unit test successful!\"}"))
           .start(port);
        try {
        	new AmazonEsRepository("testIndex").export(pages);
            MkQuery request = server.take();
            assertTrue("/es/_bulk/".equals(request.uri().toString()));
            assertTrue("POST".equals(request.method()));
        } finally {
            server.stop();
        }
    }
    
    /**
     * A request is sent to the AWS Es bulk api service, but it fails.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void sendsExportRequestToAwsEsWithError() throws Exception {
        List<WebPage> pages = new ArrayList<WebPage>();
        pages.add(this.mockWebPage("http://www.test.com/crawledpage.html"));
        pages.add(this.mockWebPage("https://www.test.com/stuff/crawledpage.html"));

        System.setProperty("aws.accessKeyId", "access_key");
        System.setProperty("aws.secretKey", "secret_key");
        System.setProperty("aws.es.region", "ro");
        int port = this.port();
        System.setProperty("aws.es.endpoint", "http://localhost:" + port + "/es/");
    
        
        MkContainer server = new MkGrizzlyContainer()
           .next(new MkAnswer.Simple(412))
           .start(port);
        try {
        	new AmazonEsRepository("testIndex").export(pages);
        } catch (AmazonServiceException ase) {
            assertTrue(ase.getErrorMessage().contains("Precondition Failed"));
            MkQuery request = server.take();
            assertTrue("/es/_bulk/".equals(request.uri().toString()));
            assertTrue("POST".equals(request.method()));
        }finally {
            server.stop();
        }
    }
    
    /**
     * A request to DELETE the index is made to ES.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void sendsDeleteRequestToAwsEs() throws Exception {
        System.setProperty("aws.accessKeyId", "access_key");
        System.setProperty("aws.secretKey", "secret_key");
        System.setProperty("aws.es.region", "ro");
        
        int port = this.port();
        System.setProperty("aws.es.endpoint", "http://localhost:" + port + "/es");

        MkContainer server = new MkGrizzlyContainer()
           .next(new MkAnswer.Simple("{\"status\":\"index deleted\"}"))
           .start(port);
        try {
        	new AmazonEsRepository("index.to.be.deleted").delete();
            MkQuery request = server.take();
            assertTrue("/es/index.to.be.deleted/".equals(request.uri().toString()));
            assertTrue("DELETE".equals(request.method()));
        } finally {
            server.stop();
        }
    }
    
    /**
     * AmazonEsRespository can tell if an index exists or not.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void tellsIfIndexExists() throws Exception {
        System.setProperty("aws.accessKeyId", "access_key");
        System.setProperty("aws.secretKey", "secret_key");
        System.setProperty("aws.es.region", "ro");
        
        int port = this.port();
        System.setProperty("aws.es.endpoint", "http://localhost:" + port + "/es");

        MkContainer server = new MkGrizzlyContainer()
           .next(new MkAnswer.Simple(HttpStatus.SC_OK))
           .start(port);
        try {
        	boolean exists = new AmazonEsRepository("present.index").exists();
        	assertTrue(exists);
            MkQuery request = server.take();
            assertTrue("/es/present.index/".equals(request.uri().toString()));
            assertTrue("HEAD".equals(request.method()));
        } finally {
            server.stop();
        }
    }
    
    /**
     * AmazonEsRespository can tell if an index exists or not.
     * @throws Exception If something goes wrong.
     */
    @Test
    public void tellsIfIndexDoesNotExist() throws Exception {
        System.setProperty("aws.accessKeyId", "access_key");
        System.setProperty("aws.secretKey", "secret_key");
        System.setProperty("aws.es.region", "ro");
        
        int port = this.port();
        System.setProperty("aws.es.endpoint", "http://localhost:" + port + "/es");

        MkContainer server = new MkGrizzlyContainer()
           .next(new MkAnswer.Simple(HttpStatus.SC_NOT_FOUND))
           .start(port);
        try {
        	boolean exists = new AmazonEsRepository("missing.index").exists();
        	assertFalse(exists);
            MkQuery request = server.take();
            assertTrue("/es/missing.index/".equals(request.uri().toString()));
            assertTrue("HEAD".equals(request.method()));
        } finally {
            server.stop();
        }
    }
    
    /**
     * Clear system properties after each test.
     */
    @After
    public void clear() {
        System.clearProperty("aws.es.bulk.endpoint");
        System.clearProperty("aws.es.region");
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretKey");
        System.clearProperty("aws.es.endpoint");
    }
    
    /**
     * Mock a WebPage for test.
     * @param url
     * @return Webpage instance.
     */
    private WebPage mockWebPage(String url) {
        WebPage page = new SnapshotWebPage();
        page.setUrl(url);

        page.setLinks(new HashSet<Link>());
        page.setTextContent("some content");
        page.setName("crawledpage.html");
        page.setTitle("crawledpage | title");
        return page;
    }
    
    /**
     * Find a free port.
     * @return A free port.
     * @throws IOException If something goes wrong.
     */
    private int port() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
