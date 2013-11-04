/**
 * Created with JetBrains WebStorm.
 * User: erisa
 * Date: 15/09/13
 * Time: 16.40
 * To change this template use File | Settings | File Templates.
 */



var Widget = angular.module('monitor.directive', []);
Widget.directive('servermap', function () {


    return {
        restrict: 'A',
        link: function (scope, element, attr) {

            element.vectorMap({
                map: 'world_en',
                backgroundColor: null,
                color: '#ffffff',
                hoverOpacity: 0.7,
                selectedColor: '#666666',
                enableZoom: true,
                showTooltip: true,
                values: sample_data,
                scaleColors: ['#C8EEFF', '#006491'],
                normalizeFunction: 'polynomial'
            });


        }
    };
});
Widget.directive('rangepicker', function () {


    return {
        require: '?ngModel',
        restrict: 'A',
        link: function (scope, element, attr, ngModel) {


            element.daterangepicker(
                {
                    ranges: {
                        'Today': [moment(), moment()],
                        'Yesterday': [moment().subtract('days', 1), moment().subtract('days', 1)],
                        'Last 7 Days': [moment().subtract('days', 6), moment()],
                        'Last 30 Days': [moment().subtract('days', 29), moment()],
                        'This Month': [moment().startOf('month'), moment().endOf('month')],
                        'Last Month': [moment().subtract('month', 1).startOf('month'), moment().subtract('month', 1).endOf('month')]
                    },
                    startDate: moment().subtract('days', 29),
                    endDate: moment()
                },
                function (start, end) {
                    var child = element.children('span')[0];
                    scope.$apply(function () {
                        ngModel.$setViewValue({ start: start, end: end});
                    });
                    $(child).html(start.format('MMMM D, YYYY') + ' - ' + end.format('MMMM D, YYYY'));
                });
            // scope[range] = { start : moment().subtract('days', 6) , end : moment()};
            ngModel.$setViewValue({ start: moment().subtract('days', 6), end: moment()});
            var child = element.children('span')[0]
            $(child).html(moment().subtract('days', 6).format('MMMM D, YYYY') + ' - ' + moment().format('MMMM D, YYYY'));
        }
    };
});

Widget.directive('metricchart', function ($http, $compile) {

    var compileChart = function (html, scope, element, attrs) {


        var chartScope = scope.$new(true);
        chartScope.metric = attrs['metricchart'];
        chartScope.chartHeight = attrs['chartheight'];
        chartScope.metricScope = scope;
        var el = angular.element($compile(html.data)(chartScope));
        element.empty();
        element.append(el);
    }

    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            $http.get('views/server/metric/singleMetric.html').then(function (response) {
                compileChart(response, scope, element, attrs);
            });
        }
    };
});
Widget.directive('rickchart', function ($http, $compile) {

    var compileChart = function (html, scope, element, attrs) {


        var chartScope = scope.$new(true);
        chartScope.metric = attrs['rickchart'];
        chartScope.chartHeight = attrs['chartheight'];
        chartScope.metricScope = scope;
        var el = angular.element($compile(html.data)(chartScope));
        element.empty();
        element.append(el);
    }

    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            $http.get('views/server/metric/rickSingleMetric.html').then(function (response) {
                compileChart(response, scope, element, attrs);
            });
        }
    };
});

Widget.directive('rickarea', function () {

    var createStackedArea = function (scope, data, element, render) {

        var graph = new Rickshaw.Graph({
            height : "500",
            series: [
                {   color : 'steelblue',
                    data: [
                    { x: 0, y: 2 },
                    { x: 1, y: 4 },
                    { x: 2, y: 4 },
                    { x: 3, y: 4 }
                ] }
            ],
            renderer: 'area',
            element: element[0]
        });
        var legend = new Rickshaw.Graph.Legend({
            graph: graph,
            element: element[0]
        });
        graph.render();

    }

    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            var data = attr.rickarea;
            var render = attr.rickrender;

            var manipulateData = function (data) {
                var keys = new Array;
                Object.keys(data).forEach(function (elem, idx, array) {
                    Object.keys(data[elem]).forEach(function (el, i, a) {
                        if (keys.indexOf(el) == -1)
                            keys.push(el);
                    });
                });
                var formatted = new Array;
                Object.keys(data).forEach(function (elem, idx, array) {
                    if (elem != "hidden") {
                        var values = new Array;
                        keys.forEach(function (e, i, a) {
                            var v = data[elem][e];
                            if (!v) {
                                v = 0;
                            }
                            values.unshift([e, v]);
                        });
                        var obj = { "key": elem, "values": values };
                        formatted.push(obj)
                    }

                });
                createStackedArea(scope, formatted, element, scope[render]);
            }
            scope.$watch(data, function (data) {
                if (data) {
                    manipulateData(data);
                }
            });
            scope.$watch(render, function (ren) {
                if (scope[data]) {
                    manipulateData(scope[data]);
                }
            })

        }
    };
});

