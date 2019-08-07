/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.template.service.rest;

import org.apache.fineract.cn.accounting.api.v1.client.LedgerManager;
import org.apache.fineract.cn.accounting.importer.AccountImporter;
import org.apache.fineract.cn.accounting.importer.LedgerImporter;
import org.apache.fineract.cn.api.context.AutoGuest;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.identity.api.v1.client.IdentityManager;
import org.apache.fineract.cn.identity.api.v1.domain.Authentication;
import org.apache.fineract.cn.template.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.template.api.v1.domain.Sample;
import org.apache.fineract.cn.template.service.ServiceConstants;
import org.apache.fineract.cn.template.service.TemplateApplication;
import org.apache.fineract.cn.template.service.internal.command.InitializeServiceCommand;
import org.apache.fineract.cn.template.service.internal.command.SampleCommand;
import org.apache.fineract.cn.template.service.internal.service.SampleService;

import java.net.URL;
import java.util.List;
import javax.validation.Valid;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/")
public class SampleRestController {

  private final Logger logger;
  private final CommandGateway commandGateway;
  private final SampleService sampleService;

  private LedgerManager ledgerManager;
  private IdentityManager identityManager;

  @Autowired
  public SampleRestController(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                              final CommandGateway commandGateway,
                              final SampleService sampleService,
                              final LedgerManager ledgerManager,
                              final IdentityManager identityManager) {
    super();
    this.logger = logger;
    this.commandGateway = commandGateway;
    this.sampleService = sampleService;

    this.ledgerManager = ledgerManager;
    this.identityManager = identityManager;
  }

  @Permittable(value = AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/initialize",
      method = RequestMethod.POST,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public
  @ResponseBody
  ResponseEntity<Void> initialize() throws InterruptedException {
      this.commandGateway.process(new InitializeServiceCommand());
      return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SAMPLE_MANAGEMENT)
  @RequestMapping(
          value = "/sample",
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public
  @ResponseBody
  List<Sample> findAllEntities() {
    return this.sampleService.findAllEntities();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SAMPLE_MANAGEMENT)
  @RequestMapping(
          value = "/sample/{identifier}",
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public
  @ResponseBody
  ResponseEntity<Sample> getEntity(@PathVariable("identifier") final String identifier) {
    return this.sampleService.findByIdentifier(identifier)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + identifier + " doesn't exist."));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SAMPLE_MANAGEMENT)
  @RequestMapping(
      value = "/sample",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public
  @ResponseBody
  ResponseEntity<Void> createEntity(@RequestBody @Valid final Sample instance) throws InterruptedException {
    this.commandGateway.process(new SampleCommand(instance));
    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SAMPLE_MANAGEMENT)
  @RequestMapping(
    value = "/import",
    method = RequestMethod.GET,
    consumes = MediaType.ALL_VALUE,
    produces = MediaType.ALL_VALUE
  )
  public
  @ResponseBody
  ResponseEntity<String> importChartOfAccounts() {
    logger.info("Import called!");
    try {
      final LedgerImporter ledgerImporter = new LedgerImporter(ledgerManager, logger);
      final URL ledgersUrl = TemplateApplication.class.getResource("/standardChartOfAccounts/ledgers.csv");
      ledgerImporter.importCSV(ledgersUrl);
      //this.eventRecorder.wait(POST_LEDGER, LOAN_INCOME_LEDGER);
      logger.info("imported ledgers");
      final AccountImporter accountImporter = new AccountImporter(ledgerManager, logger);
      final URL accountsUrl = TemplateApplication.class.getResource("/standardChartOfAccounts/accounts.csv");
      accountImporter.importCSV(accountsUrl);
      //this.eventRecorder.wait(POST_ACCOUNT, "9330");
      logger.info("imported accounts");
      return ResponseEntity.ok("");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.toString());
    }
  }
}
