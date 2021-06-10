'use strict';

function treeController() {
    let ctrl = this;
    ctrl.treeData = [];
    ctrl.reports = [];
    ctrl.treeId = Math.random().toString(36).substring(7);
    ctrl.ICONS = {
        1: "fa fa-arrow-right",
        2: "fa fa-arrow-left",
        3: "fa fa-times",
        4: "fa fa-angle-right",
        5: "fa fa-angle-left",
        6: "fa fa-info",
        7: "fa fa-plus",
        8: "fa fa-angle-double-right",
        9: "fa fa-angle-double-left"
    };

    ctrl.$onInit = function () {
        ctrl.onAddRelay.add = ctrl.addTree;
        ctrl.onSelectRelay.getPath = ctrl.getPath;
        ctrl.onSelectRelay.selectPath = ctrl.selectPath;
        ctrl.onSelectRelay.expand = ctrl.expand;
        ctrl.onSelectRelay.collapse = ctrl.collapse;
        ctrl.onSelectRelay.close = ctrl.close;
        ctrl.updateTree();
        if (ctrl.onAddRelay.hasOwnProperty("postInit") && typeof ctrl.onAddRelay.postInit === 'function') {
            ctrl.onAddRelay.postInit();
        }
    }

    ctrl.remove_circulars = function (node) {
        if ("parent" in node) {
            delete node["parent"];
        }
        if ("nodes" in node) {
            for (let i = 0; i < node["nodes"].length; i++) {
                ctrl.remove_circulars(node["nodes"][i]);
            }
        }
    };

    ctrl.downloadReports = function (exportReport, exportReportXml) {
        let queryString = "?";
        for (let i = 0; i < ctrl.treeData.length; i++) {
            queryString += "id=" + ctrl.treeData[i]["ladybug"]["storageId"] + "&";
        }
        console.info("Downloading reports with query", queryString);
        window.open("../report/download/" + ctrl.storage +
            "/" + exportReport + "/" + exportReportXml + queryString.slice(0, -1));
    }

    ctrl.getRootNode = function () {
        let tree = $('#' + ctrl.treeId).treeview(true);
        if (tree.getSelected().length === 0)
            return;
        let node = tree.getSelected()[0];
        while (node.parentId !== undefined) {
            let tempNode = tree.getNode(node.parentId);

            // In case static root node exists, it wil not have ladybug key.
            if (!("ladybug" in tempNode)) return node;

            node = tempNode
        }
        return node;
    }

    ctrl.closeAll = function () {
        ctrl.treeData = [];
        $('#' + ctrl.treeId).treeview({data: []});
    };

    ctrl.expandAll = function () {
        $('#' + ctrl.treeId).treeview('expandAll', {levels: 99, silent: true});
    }

    ctrl.collapseAll = function () {
        $('#' + ctrl.treeId).treeview('collapseAll', {silent: true});
    }

    ctrl.updateTree = function () {
        let tree = ctrl.treeData;
        if (ctrl.staticRootNode !== undefined) {
            tree = [ctrl.staticRootNode];
            tree[0].nodes = ctrl.treeData;
        }

        console.debug("Updating tree", tree);
        $('#' + ctrl.treeId).treeview({
            data: tree,
            onNodeSelected: ctrl.onSelect,
            onNodeUnselected: function (event, node) {
                ctrl.onSelect(null, null);
            }
        });
    }

    /*
     * Adds the given report data to the tree.
     */
    ctrl.addTree = function (data){
        console.info("Adding new report to tree with storage id [" + data["storageId"] + "]");
        console.debug("New report:", data);

        ctrl.reports[data["storageId"]] = data;
        let report = {
            text: data["name"],
            ladybug: data,
            icon: "fa fa-file-code",
            nodes: []
        };
        let checkpoints = data["checkpoints"];
        let nodes = report.nodes;
        let previous_node = null;
        for (let i = 0; i < checkpoints.length; i++) {
            let chkpt = checkpoints[i];
            let node = {text: chkpt["name"], ladybug: chkpt, level: chkpt["level"], icon: "fa fa-exclamation-triangle"};

            if ("encoding" in chkpt && chkpt["encoding"] === "printStackTrace()")
                node["color"] = "#ff0000";

            if (chkpt["type"] in ctrl.ICONS)
                node.icon = ctrl.ICONS[chkpt["type"]];

            if (previous_node === null) {
                nodes.push(node);
                previous_node = node;
                continue;
            }
            let diff = (node.level - 1) - previous_node.level;

            while (diff !== 0) {
                if (diff < 0) {
                    previous_node = previous_node.parent;
                } else {
                    if (!("nodes" in previous_node) || previous_node["nodes"] === null) {
                        break;
                    }
                    previous_node = previous_node.nodes[previous_node.nodes - 1];
                }
                diff = (node.level - 1) - previous_node.level;
            }
            if (!("nodes" in previous_node) || previous_node["nodes"] === null) {
                previous_node["nodes"] = [];
            }
            previous_node["nodes"].push(node);
            node.parent = previous_node;
            previous_node = node;
        }
        ctrl.remove_circulars(report);
        ctrl.treeData.push(report);

        // Update tree
        ctrl.updateTree();
    };

    ctrl.close = function () {
        let storageId = ctrl.rootNode["ladybug"]["storageId"];
        for (let i = 0; i < ctrl.treeData.length; i++) {
            if (storageId === ctrl.treeData[i]["ladybug"]["storageId"]) {
                ctrl.treeData.splice(i, 1);
                i--;
            }
        }
        ctrl.updateTree();
        ctrl.onSelect(null, null);
    }

    ctrl.onSelect = function(event, node) {
        console.debug("Selected tree node", node);
        if (ctrl.staticRootNode !== undefined && node !== null && node.nodeId === 0) {
            // Static Root Node is selected, unselect everything.
            ctrl.unselectAll();
        } else {
            ctrl.rootNode = ctrl.getRootNode();
            ctrl.selectedNode = node;
            ctrl.onSelectRelay.select(ctrl.rootNode, event, node);
        }
    }

    ctrl.unselectAll = function () {
        let tree = $("#" + ctrl.treeId).treeview(true);

        let selected = tree.getSelected();
        for (let i = 0; i < selected.length; i++)
            tree.unselectNode(selected[i]);
    }

    ctrl.getPath = function (node) {
        if (node === undefined || node === null) return undefined;
        let tree = $('#' + ctrl.treeId).treeview(true);
        let path = []
        let parent = tree.getParent(node);
        while (parent !== undefined) {
            let index = 0;
            while (index < parent.nodes.length) {
                if (parent.nodes[index].nodeId === node.nodeId) break;
                index++;
             }
            path.push(index);
            node = parent;
            parent = tree.getParent(node);
        }
        if (ctrl.staticRootNode === undefined) {
            let index = 0;
            while (index < ctrl.treeData.length) {
                if (ctrl.treeData[index].ladybug.storageId === node.ladybug.storageId) break;
                index++;
            }
            path.push(index);
        }

        return path.reverse();
    }

    ctrl.selectPath = function (path) {
        if (path === undefined || path.length === 0) return;

        console.debug("Opening node with path", path);
        let tree = $("#" + ctrl.treeId).treeview(true);
        let node;
        if (ctrl.staticRootNode === undefined) {
            node = ctrl.treeData[path[0]];
        } else {
            node = tree.getNode(0).nodes[path[0]];
        }

        if (node === undefined) return;

        for (let i = 1; i < path.length; i++) {
            if (node.nodes === undefined || node.nodes.length === 0) {
                console.warn("Requested node could not be found with the remaining path", path.splice(i));
                break;
            } else if (node.nodes.length > path[i]) {
                node = node.nodes[path[i]]
            } else {
                console.warn("Overflow while getting node with path", path[i], node);
                node = node.nodes[node.nodes.length - 1];
            }
        }
        try {
            if (!node.state.selected) {
                ctrl.unselectAll();
                tree.revealNode(node.nodeId);
                tree.selectNode(node.nodeId);
            }
        } catch (e) {
            tree.selectNode(node.nodeId);
            console.error(e);
        }
    }

    ctrl.collapse = function () {
        $('#' + ctrl.treeId).treeview('collapseNode', ctrl.selectedNode.nodeId);
    }

    ctrl.expand = function () {
        let tree = $('#' + ctrl.treeId).treeview(true);
        let node = tree.getNode(ctrl.selectedNode.nodeId);
        let path = [];
        path.push(node.nodeId);
        while (node.parentId !== undefined) {
            node = tree.getNode(node.parentId);
            path.push(node.nodeId);
        }
        for (let i = path.length - 1; i >= 0; i--) {
            tree.expandNode(path[i]);
        }
    }
}

angular.module('ladybugApp').component('reportTree', {
    templateUrl: 'components/tree/tree.html',
    controller: treeController,
    bindings: {
        onSelectRelay: '=',
        onAddRelay: '=',
        staticRootNode: '=',
        storage: '='
    }
});