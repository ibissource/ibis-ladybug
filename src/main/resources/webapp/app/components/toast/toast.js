'use strict';

function toastController() {
    let ctrl = this;

    ctrl.$onInit = function () {
        // TODO: Fix. I believe when onInit is called dom is not ready (in a sense that #toast+componentId is not set properly.
        setTimeout(ctrl.initToast, 100);
    }

    ctrl.initToast = function () {
        console.debug("Creating toast with data", {"title": ctrl.title, "text": ctrl.text, "id": ctrl.componentId});
        let toast = $('#toast' + ctrl.componentId);
        toast.toast({delay: 10000});

        // Add listener to cleanup the resources after close.
        toast.on('hidden.bs.toast', function () {
            console.log("Removing toast ", ctrl.componentId)
            $('#' + ctrl.componentId).remove();
        });

        toast.toast('show');
    }
}

angular.module('ladybugApp').component('toast', {
    templateUrl: 'components/toast/toast.html',
    controller: [toastController],
    bindings: {
        title: '@',
        text: '@',
        componentId: '@'
    }
});

var createToast = function (title, text, $scope, $compile) {
    let toastid = Math.random().toString(36).substring(7);
    $('body').append($compile("<toast title='" + title + "' text='" + text + "' " +
        "component-id='" + toastid + "' id='" + toastid + "'></toast>")($scope));
};