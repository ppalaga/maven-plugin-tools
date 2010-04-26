package org.apache.maven.tools.plugin.extractor.java;

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

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * @author jdcasey
 */
public class JavaMojoDescriptorExtractorTest
    extends TestCase
{

    private File fileOf( String classpathResource )
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource( classpathResource );

        File result = null;
        if ( resource != null )
        {
            result = FileUtils.toFile( resource );
        }

        return result;
    }

    public List extract( String directory )
        throws Exception
    {
        JavaMojoDescriptorExtractor extractor = new JavaMojoDescriptorExtractor();

        File sourceFile = fileOf( "dir-flag.txt" );

        File dir = sourceFile.getParentFile();

        Model model = new Model();
        model.setArtifactId( "maven-unitTesting-plugin" );

        MavenProject project = new MavenProject( model );

        project.setFile( new File( dir, "pom.xml" ) );
        project.addCompileSourceRoot( new File( dir, directory ).getPath() );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "test" );

        PluginToolsRequest request = new DefaultPluginToolsRequest( project, pluginDescriptor ).setEncoding( "UTF-8" );

        return extractor.execute( request );
    }

    public void testShouldFindTwoMojoDescriptorsInTestSourceDirectory()
        throws Exception
    {
        List results = extract( "source" );
        
        assertEquals( "Extracted mojos", 2, results.size() );

        for ( int i = 0; i < 2; i++ )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) results.get( i );
            assertEquals( 1, mojoDescriptor.getParameters().size() );
            Parameter parameter = (Parameter) mojoDescriptor.getParameters().get( 0 );
            assertEquals( "project", parameter.getName() );
            assertEquals( "java.lang.String[]", parameter.getType() );
        }
    }

    public void testShouldPropagateImplementationParameter()
        throws Exception
    {
        List results = extract( "source2" );

        assertEquals( 1, results.size() );

        MojoDescriptor mojoDescriptor = (MojoDescriptor) results.get( 0 );

        List parameters = mojoDescriptor.getParameters();

        assertEquals( 1, parameters.size() );

        Parameter parameter = (Parameter) parameters.get( 0 );

        assertEquals( "Implementation parameter", "source2.sub.MyBla", parameter.getImplementation() );
    }

    public void testMaven30Parameters()
        throws Exception
    {
        List results = extract( "source2" );

        assertEquals( 1, results.size() );

        ExtendedMojoDescriptor mojoDescriptor = (ExtendedMojoDescriptor) results.get( 0 );
        assertTrue( mojoDescriptor.isThreadSafe());
        assertEquals( "test", mojoDescriptor.getRequiresDependencyCollection() );

    }

    /**
     * Check that the mojo descriptor extractor will ignore any annotations that are found.
     * 
     * @throws Exception
     */
    public void testAnnotationInPlugin()
        throws Exception
    {
        List results = extract( "source3" );

        assertEquals( 0, results.size() );
    }
    
    /**
     * Check that the mojo descriptor extractor will successfully parse sources with Java 1.5 language features like
     * generics.
     */
    public void testJava15SyntaxParsing()
        throws Exception
    {
        List results = extract( "java-1.5" );

        assertEquals( 1, results.size() );
    }

}
