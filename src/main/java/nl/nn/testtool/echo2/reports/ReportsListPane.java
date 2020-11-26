/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.testtool.echo2.reports;

import nextapp.echo2.app.ContentPane;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author m00035f
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReportsListPane extends ContentPane {
    private Logger log = LogUtil.getLogger(this);
    private ReportsComponent reportsComponent;

    public void setReportsComponent(ReportsComponent reportsComponent) {
        this.reportsComponent = reportsComponent;
    }
    
	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
    public void initBean() {
        add(reportsComponent, 0);
    }
    
}
