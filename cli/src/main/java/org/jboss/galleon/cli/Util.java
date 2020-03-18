/*
 * Copyright 2016-2020 Red Hat, Inc. and/or its affiliates
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
package org.jboss.galleon.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.impl.converter.FileConverter;
import org.aesh.readline.AeshContext;
import org.aesh.readline.util.Parser;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jboss.galleon.Errors;
import org.jboss.galleon.ProvisioningException;
import org.jboss.galleon.util.PathsUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

/**
 *
 * @author Alexey Loubyansky
 */
public class Util {

    static InputStream getResourceStream(String resource) throws CommandExecutionException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream pomIs = cl.getResourceAsStream(resource);
        if(pomIs == null) {
            throw new CommandExecutionException(resource + " not found");
        }
        return pomIs;
    }

    /**
     * Creates a Plexus Container able to lookup the internal maven components using the maven ioC provider.
     * Code was taken from MavenCLI
     * @return The container.
     * @throws PlexusContainerException On failures bootstrapping the iOC container.
     * @see <a href="https://github.com/apache/maven/blob/master/maven-embedder/src/main/java/org/apache/maven/cli/MavenCli.java</a>
     */
    public static PlexusContainer createPlexusContainer() throws PlexusContainerException {
        ClassWorld classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
          ILoggerFactory slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
        ClassRealm coreRealm = classWorld.getClassRealm( "plexus.core" );
        if ( coreRealm == null )
        {
            coreRealm = classWorld.getRealms().iterator().next();
        }

        CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom( coreRealm );
        // Here you should register your Galleon extensions
        List<CoreExtensionEntry> extensions = Collections.emptyList();

        ClassRealm containerRealm = coreRealm;

          ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld( classWorld)
              .setRealm( containerRealm ).setClassPathScanning( PlexusConstants.SCANNING_INDEX ).setAutoWiring( true )
              .setJSR250Lifecycle( true ).setName( "maven" );

          Set<String> exportedArtifacts = new HashSet<>( coreEntry.getExportedArtifacts() );
          Set<String> exportedPackages = new HashSet<>( coreEntry.getExportedPackages() );
          for ( CoreExtensionEntry extension : extensions )
          {
              exportedArtifacts.addAll( extension.getExportedArtifacts() );
              exportedPackages.addAll( extension.getExportedPackages() );
          }

          final CoreExports exports = new CoreExports( containerRealm, exportedArtifacts, exportedPackages );

          DefaultPlexusContainer container = new DefaultPlexusContainer( cc, new AbstractModule()
          {
              @Override
              protected void configure() {
                  bind( ILoggerFactory.class ).toInstance( slf4jLoggerFactory );
                  bind( CoreExports.class ).toInstance( exports );
              }
          } );
          return  container;
    }

    public static RepositorySystemSession newRepositorySession(final RepositorySystem repoSystem,
            Path path, RepositoryListener listener, ProxySelector proxySelector, boolean offline) {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setRepositoryListener(listener);
        session.setOffline(offline);
        final LocalRepository localRepo = new LocalRepository(path.toString());
        session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
        if (proxySelector != null) {
            session.setProxySelector(proxySelector);
        }
        return session;
    }

    // public for testing purpose
    public static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    public static String formatColumns(List<String> lst, int width, int height) {
        String[] array = new String[lst.size()];
        lst.toArray(array);
        return Parser.formatDisplayList(array, height, width);
    }

    public static Path resolvePath(AeshContext ctx, String path) throws IOException {
        Path workDir = PmSession.getWorkDir(ctx);
        // Must be canonical due to deletion, eg:current dir is inside installation,
        // delete .., when current dir is deleted, absolute path <abs path to dir>/.. becomes invalid
        return Paths.get(new File(FileConverter.translatePath(workDir.toString(), path)).getCanonicalPath());
    }

    public static Path lookupInstallationDir(AeshContext ctx, Path install) throws ProvisioningException {
        if (install != null) {
            if (Files.exists(PathsUtils.getProvisioningXml(install))) {
                return install;
            } else {
                throw new ProvisioningException(Errors.homeDirNotUsable(install));
            }
        } else {
            Path currentDir = PmSession.getWorkDir(ctx);
            while (currentDir != null) {
                if (Files.exists(PathsUtils.getProvisioningXml(currentDir))) {
                    return currentDir;
                }
                currentDir = currentDir.getParent();
            }
            throw new ProvisioningException(Errors.homeDirNotUsable(PmSession.getWorkDir(ctx)));
        }
    }
}
