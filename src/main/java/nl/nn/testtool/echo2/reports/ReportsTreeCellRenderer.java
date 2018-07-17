/*
 * Created on 19-Mar-08
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool.echo2.reports;


import java.util.HashMap;
import java.util.Map;

import nextapp.echo2.app.Color;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.ResourceImageReference;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

import echopointng.Tree;
import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeCellRenderer;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReportsTreeCellRenderer extends DefaultTreeCellRenderer {
	private Logger log = LogUtil.getLogger(this);

	/**
	 * http://echopoint.sourceforge.net/LinkedArticles/UsingtheTreeComponent.html Advanced Tree Rendering
	 */
	public Label getTreeCellRendererText(Tree tree, Object node, boolean selected, boolean expanded, boolean leaf) {
		boolean specialForeground = false;
		Label label = null;
		// When checkpoint, create custom label
		if (node != null) {
			DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)node;
			Object userObject = defaultMutableTreeNode.getUserObject();
			if (userObject instanceof Report) {
				label = super.getTreeCellRendererText(tree, node, selected, expanded, leaf);
				Report report = (Report)userObject;
				if (report.getDifferenceChecked()) {
					if (report.getDifferenceFound()) {
						label.setForeground(Echo2Application.getDifferenceFoundLabelColor());
						specialForeground = true;
					} else {
						label.setForeground(Echo2Application.getNoDifferenceFoundLabelColor());
						specialForeground = true;
					}
				}
			} else if (userObject instanceof Checkpoint) {
				Checkpoint checkpoint = (Checkpoint)userObject;
				label = new Label();
				label.setIconTextMargin(new Extent(0));
				label.setFont(DefaultTreeCellRenderer.DEFAULT_FONT);
				label.setText(checkpoint.getName());
				if (checkpoint.getType() == Checkpoint.TYPE_STARTPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/startpoint-even.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/startpoint-odd.gif"));
					}
				} else if (checkpoint.getType() == Checkpoint.TYPE_ENDPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/endpoint-odd.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/endpoint-even.gif"));
					}
				} else if (checkpoint.getType() == Checkpoint.TYPE_ABORTPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/abortpoint-odd.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/abortpoint-even.gif"));
					}
				} else if (checkpoint.getType() == Checkpoint.TYPE_INPUTPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/inputpoint-even.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/inputpoint-odd.gif"));
					}
				} else if (checkpoint.getType() == Checkpoint.TYPE_OUTPUTPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/outputpoint-even.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/outputpoint-odd.gif"));
					}
				} else if (checkpoint.getType() == Checkpoint.TYPE_INFOPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/infopoint-even.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/infopoint-odd.gif"));
					}
				} else if (checkpoint.getType() == Checkpoint.TYPE_THREADSTARTPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/threadStartpoint-even.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/threadStartpoint-odd.gif"));
					}
				} else if (checkpoint.getType() == Checkpoint.TYPE_THREADENDPOINT) {
					if (defaultMutableTreeNode.getLevel() % 2 == 0) {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/threadEndpoint-odd.gif"));
					} else {
						label.setIcon(new ResourceImageReference("/nl/nn/testtool/echo2/reports/threadEndpoint-even.gif"));
					}
				}
			}
		}
		// When not a report or checkpoint, get default label
		if (label == null) {
//			label = super.getTreeCellRendererText(tree, node, selected, expanded, leaf);
			//TODO volgende 2 regels alleen zo voor RunPane doen?
			if (selected) expanded = true;
			label = super.getTreeCellRendererText(tree, node, selected, expanded, false);
		}
		// Customize selected color
		if (selected) {
			label.setBackground(Echo2Application.getButtonBackgroundColor());
			if (!specialForeground) {
				label.setForeground(Echo2Application.getButtonForegroundColor());
			}
		} else {
			label.setBackground(Echo2Application.getPaneBackgroundColor());
			if (!specialForeground) {
				label.setForeground(Color.BLACK);
			}
		}
		return label;
	}
}