// spec.js
describe('Ladybug simple protractor test', function() {
    it('Confirm title of ladybug app', function() {
        browser.get('http://localhost:8000');

        expect(browser.getTitle()).toEqual('Ladybug');
    });
});

describe('Go through each of the tabs', function () {

    browser.get('http://localhost:8000');

    it('Click on the Test tab', function () {
        let expectedUrl = 'http://localhost:8000/#!/view2';
        $('#view2').click()
        expect(browser.getCurrentUrl()).toEqual(expectedUrl);
    });

    it('Click on the Compare tab', function () {
        let expectedUrl = 'http://localhost:8000/#!/view3';
        $('#view3').click()
        expect(browser.getCurrentUrl()).toEqual(expectedUrl);
    });

    it('Click on the Debug tab', function () {
        let expectedUrl = 'http://localhost:8000/#!/view1';
        $('#view1').click()
        expect(browser.getCurrentUrl()).toEqual(expectedUrl);
    });
})