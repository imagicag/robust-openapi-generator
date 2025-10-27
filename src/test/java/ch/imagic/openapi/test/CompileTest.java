// Copyright 2025 Imagic Bildverarbeitung AG CH-8152 Glattbrugg
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package ch.imagic.openapi.test;

import ch.imagic.openapi.OpenApiGenerator;
import ch.imagic.openapi.OpenApiGeneratorConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * This test generates a api from a schema and compiles it.
 * It does not test that the generated code is any good, it just tests that it compiles.
 */
public class CompileTest {

    private static void rmdir(File dir) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                f.delete();
                rmdir(f);
            }
        }
        dir.delete();
    }

    @Before
    public void b4() {
        rmdir(new File("testproject/src/main/java/undertest"));
    }

    @Test
    public void test() throws Exception {
        OpenApiGeneratorConfig config = new OpenApiGeneratorConfig();
        config.setSchema(new File("petstore-expanded.json"));
        config.setPackageName("undertest");
        File sourceDir = new File("testproject/src/main/java");
        config.setCommonPackageName("undertest.common");
        config.setApiSourceTargetDir(sourceDir);
        config.setImplSourceTargetDir(sourceDir);
        config.setCommonImplSourceTargetDir(sourceDir);
        config.setCommonApiSourceTargetDir(sourceDir);

        OpenApiGenerator.generate(config);

        String javaHome = System.getProperty("java.home");

        //Poor mans windows detection.
        if (File.separatorChar == '\\') {
            // TODO test if this actually works or not...
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "mvnw.cmd clean package");
            pb.directory(new File("testproject").getAbsoluteFile());
            pb.inheritIO();
            pb.environment().put("JAVA_HOME", javaHome);
            Assert.assertEquals(0, pb.start().waitFor());
            return;
        }
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/env", "bash", "mvnw", "clean", "package");
        pb.directory(new File("testproject").getAbsoluteFile());
        pb.environment().put("JAVA_HOME", javaHome);
        pb.inheritIO();
        Assert.assertEquals(0, pb.start().waitFor());

    }
}
