/*==============================================================================
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
==============================================================================*/
package eu.delving.x3ml;

import gr.forth.ics.isl.x3ml.X3MLEngine;
import gr.forth.ics.isl.x3ml.X3MLGeneratorPolicy;
import gr.forth.ics.isl.x3ml.engine.Generator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import java.util.List;
import static eu.delving.x3ml.AllTests.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 * @author Nikos Minadakis <minadakn@ics.forth.gr>
 * @author Yannis Marketakis <marketak@ics.forth.gr>
 */

public class TestGML {
    private final Logger log = Logger.getLogger(getClass());

    @Test
    public void testGML() {
        X3MLEngine engine = engine("/gml/gml-1.x3ml");
        Generator policy = X3MLGeneratorPolicy.load(resource("/coin_a/00-generator-policy.xml"), X3MLGeneratorPolicy.createUUIDSource(1));
        X3MLEngine.Output output = engine.execute(document("/gml/gml-1-input.xml"), policy);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/gml/gml-1-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\nLINES:"+ diff.size() + "\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }


}
