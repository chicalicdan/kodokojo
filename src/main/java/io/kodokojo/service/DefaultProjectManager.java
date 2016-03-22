package io.kodokojo.service;

/*
 * #%L
 * project-manager
 * %%
 * Copyright (C) 2016 Kodo-kojo
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import io.kodokojo.commons.model.Service;
import io.kodokojo.commons.utils.ssl.SSLKeyPair;
import io.kodokojo.commons.utils.ssl.SSLUtils;
import io.kodokojo.model.*;
import io.kodokojo.project.starter.BrickManager;
import io.kodokojo.service.dns.DnsEntry;
import io.kodokojo.service.dns.DnsManager;
import org.apache.commons.collections4.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isBlank;

public class DefaultProjectManager implements ProjectManager {

    private final DnsManager dnsManager;

    private final BrickManager brickManager;

    private final SSLKeyPair caKey;

    private final String domain;

    private final ConfigurationStore configurationStore;

    private final ProjectStore projectStore;

    private final BootstrapConfigurationProvider bootstrapConfigurationProvider;

    private final long sslCaDuration;

    @Inject
    public DefaultProjectManager(SSLKeyPair caKey, String domain, DnsManager dnsManager, BrickManager brickManager, ConfigurationStore configurationStore, ProjectStore projectStore,BootstrapConfigurationProvider bootstrapConfigurationProvider, long sslCaDuration) {
        if (caKey == null) {
            throw new IllegalArgumentException("caKey must be defined.");
        }
        if (isBlank(domain)) {
            throw new IllegalArgumentException("domain must be defined.");
        }
        if (dnsManager == null) {
            throw new IllegalArgumentException("dnsManager must be defined.");
        }
        if (configurationStore == null) {
            throw new IllegalArgumentException("configurationStore must be defined.");
        }
        if (brickManager == null) {
            throw new IllegalArgumentException("brickManager must be defined.");
        }
        if (projectStore == null) {
            throw new IllegalArgumentException("projectStore must be defined.");
        }
        if (bootstrapConfigurationProvider == null) {
            throw new IllegalArgumentException("bootstrapConfigurationProvider must be defined.");
        }
        this.dnsManager = dnsManager;
        this.brickManager = brickManager;
        this.caKey = caKey;
        this.domain = domain;
        this.configurationStore = configurationStore;
        this.projectStore = projectStore;
        this.bootstrapConfigurationProvider = bootstrapConfigurationProvider;
        this.sslCaDuration = sslCaDuration;
    }

    @Override
    public BootstrapStackData bootstrapStack(String projectName, StackConfiguration stackConfiguration) {
        if (!projectStore.projectNameIsValid(projectName)) {
            throw new IllegalArgumentException("project name " + projectName + " isn't valid.");
        }
        String loadBalancerIp = bootstrapConfigurationProvider.provideLoadBalancerIp(projectName, stackConfiguration.getName());
        int sshPortEntrypoint = 0;
        if (stackConfiguration.getType() == StackType.BUILD) {
            bootstrapConfigurationProvider.provideSshPortEntrypoint(projectName, stackConfiguration.getName());
        }
        BootstrapStackData res = new BootstrapStackData(projectName, stackConfiguration.getName(), loadBalancerIp, sshPortEntrypoint);
        configurationStore.storeBootstrapStackData(res);
        return res;
    }

    @Override
    public Project start(ProjectConfiguration projectConfiguration) {
        if (projectConfiguration == null) {
            throw new IllegalArgumentException("projectConfiguration must be defined.");
        }
        if (CollectionUtils.isEmpty(projectConfiguration.getStackConfigurations())) {
            throw new IllegalArgumentException("Unable to create a project without stack.");
        }

        String projectName = projectConfiguration.getName();
        String projectDomainName = projectName + "." + domain;
        SSLKeyPair projectCaSSL = SSLUtils.createSSLKeyPair(projectDomainName, caKey.getPrivateKey(), caKey.getPublicKey(), caKey.getCertificates(), sslCaDuration, true);

        Set<Stack> stacks = new HashSet<>();
        for (StackConfiguration stackConfiguration : projectConfiguration.getStackConfigurations()) {
            Set<BrickDeploymentState> brickEntities = new HashSet<>();
            String lbIp = stackConfiguration.getLoadBalancerIp();

            for (BrickConfiguration brickConfiguration : stackConfiguration.getBrickConfigurations()) {
                BrickType brickType = brickConfiguration.getType();
                if (brickType.isRequiredHttpExposed()) {
                    String brickTypeName = brickType.name().toLowerCase();
                    String brickDomainName = brickTypeName + "." + projectDomainName;
                    dnsManager.createDnsEntry(new DnsEntry(brickDomainName, DnsEntry.Type.A, lbIp));
                    SSLKeyPair brickSslKeyPair = SSLUtils.createSSLKeyPair(brickDomainName, projectCaSSL.getPrivateKey(), projectCaSSL.getPublicKey(), projectCaSSL.getCertificates());
                    configurationStore.storeSSLKeys(projectName, brickTypeName, brickSslKeyPair);
                }
                Set<Service> services = brickManager.start(projectConfiguration, brickType);
                brickManager.configure(projectConfiguration, brickType);
                BrickDeploymentState brickDeploymentState = new BrickDeploymentState(brickConfiguration.getBrick(), new ArrayList<>(services), 1);
                brickEntities.add(brickDeploymentState);
            }

            Stack stack = new Stack(stackConfiguration.getName(), stackConfiguration.getType(), brickManager.getOrchestratorType(), brickEntities);
            stacks.add(stack);

        }

        return new Project(projectName, projectCaSSL, new Date(), stacks);
    }
}