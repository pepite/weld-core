/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.tests.contexts.conversation;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.weld.tests.category.Integration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

/**
 * @author Nicklas Karlsson
 * @author Dan Allen
 */
@Category(Integration.class)
@RunWith(Arquillian.class)
@Run(RunModeType.AS_CLIENT)
public class ClientConversationContextTest 
{

   public static final String CID_REQUEST_PARAMETER_NAME = "cid";

   public static final String CID_HEADER_NAME = "org.jboss.jsr299.tck.cid";

   public static final String LONG_RUNNING_HEADER_NAME = "org.jboss.jsr299.tck.longRunning";

   @Deployment
   public static WebArchive createDeployment() 
   {
      return ShrinkWrap.create(WebArchive.class, "test.war")
               .addClasses(ConversationTestPhaseListener.class, Cloud.class, Thunderstorm.class, Hailstorm.class)
               .addWebResource(ClientConversationContextTest.class.getPackage(), "web.xml", "web.xml")
               .addWebResource(ClientConversationContextTest.class.getPackage(), "faces-config.xml", "faces-config.xml")
               .addResource(ClientConversationContextTest.class.getPackage(), "cloud.xhtml", "cloud.xhtml")
               .addResource(ClientConversationContextTest.class.getPackage(), "thunderstorm.xhtml", "thunderstorm.xhtml")
               .addResource(ClientConversationContextTest.class.getPackage(), "hailstorm.xhtml", "hailstorm.xhtml")
               .addResource(ClientConversationContextTest.class.getPackage(), "blizzard.xhtml", "blizzard.xhtml")
               .addWebResource(EmptyAsset.INSTANCE, "beans.xml");
   }
   
   @Test
   public void testConversationNotPropagatedByHLink() throws Exception
   {
      WebClient client = new WebClient();
    
      // Access the start page
      HtmlPage cloud = client.getPage(getPath("/cloud.jsf"));
      String cloudName = getFirstMatchingElement(cloud, HtmlSpan.class, "cloudName").getTextContent();
      assertEquals(Cloud.NAME, cloudName);
      
      // Now start a conversation and check the cloud name changes
      HtmlPage blizzard = getFirstMatchingElement(cloud, HtmlSubmitInput.class, "blizzard").click();
      cloudName = getFirstMatchingElement(blizzard, HtmlSpan.class, "cloudName").getTextContent();
      assertEquals("henry", cloudName);
      
      // Now use the h:link to navigate back and check the conversation isn't propagated
      cloud = getFirstMatchingElement(blizzard, HtmlAnchor.class, "cloud-link").click();
      cloudName = getFirstMatchingElement(cloud, HtmlSpan.class, "cloudName").getTextContent();
      assertEquals(Cloud.NAME, cloudName);
   }
   
   @Test
   public void testConversationPropagationToNonExistentConversationLeadsException() throws Exception
   {
      WebClient client = new WebClient();
      client.setThrowExceptionOnFailingStatusCode(false);
      Page page = client.getPage(getPath("/cloud.jsf", "org.jboss.jsr299"));
      
      Assert.assertEquals(500, page.getWebResponse().getStatusCode());
   }
   
   @Test
   public void testExceptionInPreDestroy() throws Exception
   {
      WebClient client = new WebClient();
      
      // First, try a transient conversation
      
      // Access a page that throws an exception
      client.getPage(getPath("/thunderstorm.jsf"));
      
      // Then access another page that doesn't and check the contexts are ok
      HtmlPage cloud = client.getPage(getPath("/cloud.jsf"));
      String cloudName = getFirstMatchingElement(cloud, HtmlSpan.class, "cloudName").getTextContent();
      assertEquals(Cloud.NAME, cloudName);
      
      // Now start a conversation and access the page that throws an exception again
      HtmlPage thunderstorm = getFirstMatchingElement(cloud, HtmlSubmitInput.class, "beginConversation").click();
      
      String thunderstormName = getFirstMatchingElement(thunderstorm, HtmlSpan.class, "thunderstormName").getTextContent();
      assertEquals(Thunderstorm.NAME, thunderstormName);
      cloud = getFirstMatchingElement(thunderstorm, HtmlSubmitInput.class, "cloud").click();
      
      // And navigate to another page, checking the conversation exists by verifying that state is maintained
      cloudName = getFirstMatchingElement(cloud, HtmlSpan.class, "cloudName").getTextContent();
      assertEquals("bob", cloudName);
   }
   
   @Test
   public void testExceptionInPostConstruct() throws Exception
   {
      WebClient client = new WebClient();
      
      // First, try a transient conversation
      
      client.setThrowExceptionOnFailingStatusCode(false);
      
      // Access a page that throws an exception
      client.getPage(getPath("/hailstorm.jsf"));
      
      // Then access another page that doesn't and check the contexts are ok
      HtmlPage cloud = client.getPage(getPath("/cloud.jsf"));
      String cloudName = getFirstMatchingElement(cloud, HtmlSpan.class, "cloudName").getTextContent();
      assertEquals(Cloud.NAME, cloudName);
      
      // Now start a conversation and access the page that throws an exception again
      HtmlPage hailstorm = getFirstMatchingElement(cloud, HtmlSubmitInput.class, "hail").click();
      
      String cid = getCid(hailstorm);
      
      cloud = client.getPage(getPath("/cloud.jsf", cid));
      
      // And navigate to another page, checking the conversation exists by verifying that state is maintained
      cloudName = getFirstMatchingElement(cloud, HtmlSpan.class, "cloudName").getTextContent();
      assertEquals("gavin", cloudName);
   }

   protected String getPath(String viewId, String cid)
   {
      // TODO: this should be moved out and be handled by Arquillian
      return "http://localhost:8080/test" + viewId + "?" + CID_REQUEST_PARAMETER_NAME + "=" + cid;
   }
   
   protected String getPath(String viewId)
   {
      // TODO: this should be moved out and be handled by Arquillian
      return "http://localhost:8080/test" + viewId;
   }
   
   protected <T> Set<T> getElements(HtmlElement rootElement, Class<T> elementClass)
   {
     Set<T> result = new HashSet<T>();
     
     for (HtmlElement element : rootElement.getAllHtmlChildElements())
     {
        result.addAll(getElements(element, elementClass));
     }
     
     if (elementClass.isInstance(rootElement))
     {
        result.add(elementClass.cast(rootElement));
     }
     return result;
     
   }
   
   protected String getCid(Page page)
   {
      String url = page.getWebResponse().getRequestUrl().toString();
      return url.substring(url.indexOf("cid=") + 4);
   }
 
   protected <T extends HtmlElement> T getFirstMatchingElement(HtmlPage page, Class<T> elementClass, String id)
   {
     
     Set<T> inputs = getElements(page.getBody(), elementClass);
     for (T input : inputs)
     {
         if (input.getId().contains(id))
         {
            return input;
         }
     }
     return null;
   }

}