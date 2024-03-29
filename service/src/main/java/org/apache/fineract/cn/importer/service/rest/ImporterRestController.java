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
package org.apache.fineract.cn.importer.service.rest;

import org.apache.fineract.cn.accounting.api.v1.client.LedgerManager;
import org.apache.fineract.cn.accounting.importer.AccountImporter;
import org.apache.fineract.cn.accounting.importer.LedgerImporter;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.importer.service.ImporterApplication;
import org.apache.fineract.cn.importer.service.ServiceConstants;
import org.apache.fineract.cn.importer.service.internal.command.InitializeServiceCommand;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/")
public class ImporterRestController {

  private final Logger logger;
  private final CommandGateway commandGateway;

  private LedgerManager ledgerManager;

  @Autowired
  public ImporterRestController(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                final CommandGateway commandGateway,
                                final LedgerManager ledgerManager) {
    super();
    this.logger = logger;
    this.commandGateway = commandGateway;

    this.ledgerManager = ledgerManager;
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

  @Permittable(value = AcceptedTokenType.TENANT, groupId = ServiceConstants.IMPORT_PERMITTABLE_GROUP)
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
      final URL ledgersUrl = ImporterApplication.class.getResource("/standardChartOfAccounts/ledgers.csv");
      ledgerImporter.importCSV(ledgersUrl);
      //this.eventRecorder.wait(POST_LEDGER, LOAN_INCOME_LEDGER);
      logger.info("imported ledgers");
      final AccountImporter accountImporter = new AccountImporter(ledgerManager, logger);
      final URL accountsUrl = ImporterApplication.class.getResource("/standardChartOfAccounts/accounts.csv");
      accountImporter.importCSV(accountsUrl);
      //this.eventRecorder.wait(POST_ACCOUNT, "9330");
      logger.info("imported accounts");
      return ResponseEntity.ok("");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.toString());
    }
  }
}
