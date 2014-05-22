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
                    opens: 'left',
                    timePicker: true,
                    startDate: moment().subtract('days', 29),
                    endDate: moment()
                },
                function (start, end) {
                    var child = element.children('span')[0];
                    scope.$apply(function () {
                        ngModel.$setViewValue({ start: start, end: end});
                    });
                    $(child).html(start.format('MMM D, YYYY') + ' - ' + end.format('MMM D, YYYY'));
                });

            var child = element.children('span')[0]
            if (scope["range"]) {
                ngModel.$setViewValue();
                $(child).html(scope["range"].start.format('MMM D, YYYY') + ' - ' + scope["range"].end.format('MMM D, YYYY'));
            } else {
                ngModel.$setViewValue({ start: moment().subtract('days', 6), end: moment()});
                $(child).html(moment().subtract('days', 6).format('MMM D, YYYY') + ' - ' + moment().format('MMM D, YYYY'));
            }


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
            height: "500",
            series: [
                {   color: 'steelblue',
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

Widget.directive('numbersonly', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, modelCtrl) {
            modelCtrl.$parsers.push(function (inputValue) {
                // this next if is necessary for when using ng-required on your input.
                // In such cases, when a letter is typed first, this parser will be called
                // again, and the 2nd time, the value will be undefined
                if (inputValue == undefined) return ''
                var transformedInput = inputValue.replace(/[^0-9]/g, '');
                if (transformedInput != inputValue) {
                    modelCtrl.$setViewValue(transformedInput);
                    modelCtrl.$render();
                }

                return transformedInput;
            });
        }
    };
});


Widget.directive('servergraph', function () {

    var drawGrap = function (scope, element, attrs, model) {

        var width = 960,
            height = 400,
            colors = d3.scale.category10();
        var svg = d3.select(element[0]).append('svg').attr('width', width)
            .attr('height', height);
        ;

        var selected_node = null,
            selected_link = null,
            mousedown_link = null,
            mousedown_node = null,
            mouseup_node = null;
        var nodes = [
            {id: 0, reflexive: false},
            {id: 1, reflexive: true },
            {id: 2, reflexive: false}
        ];
        var lastNodeId = 2;
        var links = [
            {source: nodes[0], target: nodes[1], left: false, right: true },
            {source: nodes[1], target: nodes[2], left: false, right: true }
        ];

        var force = d3.layout.force()
            .nodes(nodes)
            .links(links)
            .size([width, height])
            .linkDistance(150)
            .charge(-500)
            .on('tick', tick)


        var circle = svg.append('svg:g').selectAll('g');

        circle = circle.data(nodes, function (d) {
            return d.id;
        });

        circle.selectAll('circle')
            .style('fill', function (d) {
                return (d === selected_node) ? d3.rgb(colors(d.id)).brighter().toString() : colors(d.id);
            })
            .classed('reflexive', function (d) {
                return d.reflexive;
            });

        var g = circle.enter().append('svg:g');
        g.append('svg:circle')
            .attr('class', 'node')
            .attr('r', 12)
            .style('fill', function (d) {
                return (d === selected_node) ? d3.rgb(colors(d.id)).brighter().toString() : colors(d.id);
            })
            .style('stroke', function (d) {
                return d3.rgb(colors(d.id)).darker().toString();
            })
            .classed('reflexive', function (d) {
                return d.reflexive;
            });

        g.append('svg:text')
            .attr('x', 0)
            .attr('y', 4)
            .attr('class', 'id')
            .text(function (d) {
                return d.id;
            });
        // remove old nodes
        function tick() {

            circle.attr('transform', function (d) {
                return 'translate(' + d.x + ',' + d.y + ')';
            });
        }

        circle.exit().remove();
        force.start();
    }
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, modelCtrl) {

            scope.$watch(function () {
                return modelCtrl.$modelValue;
            }, function (modelValue) {
                if (modelValue) {
                    drawGrap(scope, element, attrs, modelValue);
                }

            })

        }
    };
});

