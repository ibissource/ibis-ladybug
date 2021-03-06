/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool.test.junit;

import junit.framework.Test;
import junit.framework.TestSuite;
import nl.nn.testtool.test.junit.createreport.TestCreateReport;
import nl.nn.testtool.test.junit.rerun.TestRerun;
import nl.nn.testtool.test.junit.util.TestExport;
import nl.nn.testtool.test.junit.util.TestImport;
import nl.nn.testtool.test.junit.util.TestSearchUtil;

public class AllTests extends TestSuite {

	public static Test suite() {
		TestSuite testSuite = new TestSuite("All tests");
		try {
			testSuite.addTest(new TestSuite(TestCreateReport.class));
			testSuite.addTest(new TestSuite(TestRerun.class));
			testSuite.addTest(new TestSuite(TestExport.class));
			testSuite.addTest(new TestSuite(TestImport.class));
			testSuite.addTest(new TestSuite(TestSearchUtil.class));
			testSuite.addTest(new TestSuite(TestMessageEncoder.class));
		} catch(Throwable t) {
			System.out.println("Caught throwable adding test suites: " + t.getMessage());
			t.printStackTrace(System.out);
		}
		return testSuite;
	}
}
