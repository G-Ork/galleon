/*
 * Copyright 2016-2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.galleon.layout.test;

import org.jboss.galleon.ProvisioningDescriptionException;
import org.jboss.galleon.ProvisioningException;
import org.jboss.galleon.config.ProvisioningConfig;
import org.jboss.galleon.creator.FeaturePackCreator;
import org.jboss.galleon.layout.LayoutOrderingTestBase;
import org.jboss.galleon.universe.FeaturePackLocation;
import org.jboss.galleon.universe.FeaturePackLocation.FPID;
import org.jboss.galleon.universe.MvnUniverse;

/**
 *
 * @author Alexey Loubyansky
 */
public class HigherInHeirarchyVersionOverridesLowerInHeirarchyVersionTestCase extends LayoutOrderingTestBase {

    private FeaturePackLocation fpl1_100;
    private FeaturePackLocation fpl1_101;
    private FeaturePackLocation fpl2;
    private FeaturePackLocation fpl3;

    @Override
    protected void createProducers(MvnUniverse universe) throws ProvisioningException {
        universe.createProducer("prod1");
        universe.createProducer("prod2");
        universe.createProducer("prod3");
    }

    @Override
    protected void createFeaturePacks(FeaturePackCreator creator) throws ProvisioningDescriptionException {
        fpl1_100 = newFpl("prod1", "1", "1.0.0.Final");
        creator.newFeaturePack(fpl1_100.getFPID());

        fpl1_101 = newFpl("prod1", "1", "1.0.1.Final");
        creator.newFeaturePack(fpl1_101.getFPID());

        fpl2 = newFpl("prod2", "1", "1.0.0.Final");
        creator.newFeaturePack(fpl2.getFPID()).addDependency(fpl1_101);

        fpl3 = newFpl("prod3", "1", "1.0.0.Final");
        creator.newFeaturePack(fpl3.getFPID()).addDependency(fpl2).addDependency(fpl1_100);
    }

    @Override
    protected ProvisioningConfig provisioningConfig() throws ProvisioningException {
        return ProvisioningConfig.builder()
                .addFeaturePackDep(fpl3)
                .build();
    }

    @Override
    protected FPID[] expectedOrder() {
        return new FPID[] {fpl2.getFPID(), fpl1_100.getFPID(), fpl3.getFPID()};
    }
}
