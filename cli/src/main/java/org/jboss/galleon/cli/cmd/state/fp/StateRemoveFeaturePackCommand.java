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
package org.jboss.galleon.cli.cmd.state.fp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Argument;
import org.jboss.galleon.ProvisioningException;
import org.jboss.galleon.cli.AbstractCompleter;
import org.jboss.galleon.cli.CommandExecutionException;
import org.jboss.galleon.cli.HelpDescriptions;
import org.jboss.galleon.cli.PmCommandInvocation;
import org.jboss.galleon.cli.PmCompleterInvocation;
import org.jboss.galleon.cli.cmd.CommandDomain;
import org.jboss.galleon.cli.cmd.state.AbstractStateCommand;
import org.jboss.galleon.cli.model.state.State;
import org.jboss.galleon.config.FeaturePackConfig;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "remove-dependency", description = HelpDescriptions.REMOVE_DEPENDENCY, activator = RemoveFeaturePackCommandActivator.class)
public class StateRemoveFeaturePackCommand extends AbstractStateCommand {

    public static class ProvisionedFPCompleter extends AbstractCompleter {

        @Override
        protected List<String> getItems(PmCompleterInvocation completerInvocation) {
            State session = completerInvocation.getPmSession().getState();
            List<String> lst = new ArrayList<>();
            if (session != null) {
                for (FeaturePackConfig fp : session.getConfig().getFeaturePackDeps()) {
                    String loc = completerInvocation.getPmSession().
                            getExposedLocation(null, fp.getLocation()).toString();
                    lst.add(loc);
                }
            }
            return lst;
        }

    }
    @Argument(completer = ProvisionedFPCompleter.class, description = HelpDescriptions.FP_TO_REMOVE)
    protected String fpl;

    @Override
    protected void runCommand(PmCommandInvocation invoc, State session) throws IOException, ProvisioningException, CommandExecutionException {
        session.removeDependency(invoc.getPmSession(), invoc.getPmSession().getResolvedLocation(null, fpl));
    }

    @Override
    public CommandDomain getDomain() {
        return CommandDomain.EDITING;
    }

}
