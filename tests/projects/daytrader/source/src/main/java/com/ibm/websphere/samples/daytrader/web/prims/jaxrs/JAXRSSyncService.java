/**
 * (C) Copyright IBM Corporation 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.websphere.samples.daytrader.web.prims.jaxrs;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@ApplicationPath("/jaxrs")
@Path("sync")
public class JAXRSSyncService {

  /**
   * note: this should be the basic code path for jaxrs process
   * @param input
   * @return
   */
  @GET
  @Path("echoText")   
  public String echoString(@QueryParam("input") String input) {
    return input;
  }

  /**
   *  note: this code path involves JSON marshaller & un-marshaller based on basic code path
   * @param p  Person Object
   * @return  Person Object
   */
  @POST
  @Path("echoJSON")
  @Produces(value={MediaType.APPLICATION_JSON})
  @Consumes(value={MediaType.APPLICATION_JSON})
  public TestJSONObject echoObject(TestJSONObject jsonObject) {
    return jsonObject;
  }

  @POST
  @Path("echoXML")
  @Produces(value={MediaType.TEXT_XML,MediaType.APPLICATION_XML})
  @Consumes(value={MediaType.TEXT_XML,MediaType.APPLICATION_XML})
  public XMLObject echoObject(XMLObject xmlObject) {
    return xmlObject;
  }
}

