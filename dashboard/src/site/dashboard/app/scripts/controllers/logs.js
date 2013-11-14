var dbModule = angular.module('workbench-logs.controller', ['workbench-logs.services']);
dbModule.controller("LogsController", ['$scope', '$http', '$location', '$routeParams', 'CommandLogApi', 'Monitor', function ($scope, $http, $location, $routeParams, CommandLogApi, Monitor) {
    $scope.countPage = 1000;
    $scope.countPageOptions = [100, 500, 1000];
//  LOG_LEVEL.ERROR.ordinal() 4
//  LOG_LEVEL.CONFIG.ordinal() 7
//	LOG_LEVEL.DEBUG.ordinal() 0
//	LOG_LEVEL.INFO.ordinal() 1
//	LOG_LEVEL.WARN.ordinal() 3
    $scope.types = ['CONFIG', 'DEBUG', 'ERROR', 'INFO', 'WARN'];
    $scope.files = ['ALL_FILES', 'LAST'];
    $scope.selectedType = undefined;
    $scope.selectedFile = undefined;


    $scope.$watch("server", function (data) {

        if (data) {
            CommandLogApi.getListFiles({server: $scope.server.name }, function (data) {
                if (data) {
                    for (entry in data['files']) {
                        $scope.files.push(data['files'][entry]['name']);
                    }
                }
            });
        }
    });

    $scope.results = undefined;
    $scope.selectedSearch = '';
    $scope.getListFiles = function () {
        CommandLogApi.getListFiles({server: $scope.server }, function (data) {

            if (data) {
                for (entry in data['files']) {
                    $scope.files.push(data['files'][entry]['name']);
                }
            }
        });
    }
    $scope.$watch("countPage", function (data) {
        if ($scope.resultTotal) {
            $scope.results = $scope.resultTotal.logs.slice(0, $scope.countPage);
            $scope.currentPage = 1;
            $scope.numberOfPage = new Array(Math.ceil($scope.resultTotal.logs.length / $scope.countPage));
        }
    });
    $scope.clear = function () {
        $scope.queries = new Array;
    }
    $scope.switchPage = function (index) {
        if (index != $scope.currentPage) {
            $scope.currentPage = index;
            $scope.results = $scope.resultTotal.logs.slice(
                (index - 1) * $scope.countPage,
                index * $scope.countPage
            );
        }
    }

    $scope.checkDateFrom = function () {
        if ($scope.selectedDateFrom == undefined || $scope.selectedDateFrom == '') {
            return true;
        }
        return false
    }
    $scope.checkHourFrom = function () {
        if ($scope.selectedHourFrom == undefined || $scope.selectedHourFrom == '') {
            return true;
        }
        return false
    }
    $scope.checkFile = function () {
        if ($scope.selectedFile == 'LAST') {
            return true;
        }
        return false;
    }
    $scope.previous = function () {
        if ($scope.currentPage > 1) {
            $scope.switchPage($scope.currentPage - 1);
        }
    }
    $scope.next = function () {

        if ($scope.currentPage < $scope.numberOfPage.length) {
            $scope.switchPage($scope.currentPage + 1);
        }
    }
    $scope.search = function () {

        var typeofS = undefined;
        var filess = undefined;
        if ($scope.selectedFile == undefined || $scope.selectedFile == '') {
            return;
        }
        if ($scope.selectedFile == 'ALL_FILES') {
            typeofS = 'search';
        }
        else if ($scope.selectedFile == 'LAST') {
            typeofS = 'tail';
        }
        else {
            typeofS = 'file';
            filess = $scope.selectedFile
        }
        CommandLogApi.getLastLogs({server: $scope.server.name, file: filess, typeofSearch: typeofS, searchvalue: $scope.selectedSearch, logtype: $scope.selectedType, dateFrom: $scope.selectedDateFrom, hourFrom: $scope.selectedHourFrom, dateTo: $scope.selectedDateTo, hourTo: $scope.selectedHourTo}, function (data) {
            if (data) {
                $scope.resultTotal = data;
                $scope.results = data.logs.slice(0, $scope.countPage);
                $scope.currentPage = 1;
                $scope.numberOfPage = new Array(Math.ceil(data.logs.length / $scope.countPage));
            }
        });
    }
    $scope.selectType = function (selectedType) {
        $scope.selectedType = selectedType;
    }
    $scope.clearType = function () {
        $scope.selectedType = undefined;
    }
    $scope.clearSearch = function () {
        $scope.selectedSearch = undefined;
    }

}]);

dbModule.controller("LogsJavaController", ['$scope', '$http', '$location', '$routeParams', 'CommandLogApi', '$modal', '$q', 'Monitor', function ($scope, $http, $location, $routeParams, CommandLogApi, $modal, $q, Monitor) {

    var sql = "select * from Log fetchPlan *:1";

    $scope.level = undefined;
    $scope.description = undefined;
    $scope.date = undefined;
    $scope.levels = ['CONFIG', 'DEBUG', 'ERROR', 'INFO', 'WARN'];
    $scope.selectedDateFrom = undefined;
    $scope.selectedHourFrom = undefined;
    $scope.selectedDateTo = undefined;
    $scope.selectedHourTo = undefined;
    $scope.countPage = 1000;
    $scope.countPageOptions = [1000, 500, 100];

    $scope.metadata = CommandLogApi.refreshMetadata('monitor', function (data) {
    });

    $scope.getJavaLogs = function () {
        CommandLogApi.queryText({database: $routeParams.database, limit: -1, language: 'sql', text: sql, shallow: 'shallow'}, function (data) {
            if (data) {
                $scope.headers = CommandLogApi.getPropertyTableFromResults(data.result);
                console.log(data)
                $scope.resultTotal = data;
                $scope.results = data.result.slice(0, $scope.countPage);
                $scope.currentPage = 1;
                $scope.numberOfPage = new Array(Math.ceil(data.result.length / $scope.countPage));
            }
        });
    }
    $scope.getJavaLogs();
    $scope.parseTime = function (parsehour) {

        if (parsehour != undefined && parsehour != null && parsehour != '') {
            var hour = parsehour.split(" ");
            var hh = hour[0].split(":")[0]
            var mm = hour[0].split(":")[1];
            if (hour[1].contains('PM')) {
                hh = parseInt(hh) + 12;
            }
            return ' ' + hh + ':' + mm + ':00'
        }
        return '';
    }

    $scope.purgeLogs = function () {

        Utilities.confirm($scope, $modal, $q, {

            title: 'Warning!',
            body: 'You are dropping all Workench log. Are you sure?',
            success: function () {
                CommandLogApi.purge({type: 'logs'}, function (data) {

                    $scope.getJavaLogs();

                });
            }
        });
    }
    $scope.search = function () {
        var day = moment($scope.selectedDateFrom);

        var hourFrom = moment(new Date());

        var first = true
        var sql = "select * from Log ";

        if ($scope.level != undefined && $scope.level != null) {
            var sqlapp = "WHERE levelDescription = '" + $scope.level + "'";

            sql = sql.concat(sqlapp);
            first = false;
        }
        if ($scope.description != undefined && $scope.description != null && $scope.description != '') {
            if (!first) {
                var sqlapp = " and  message like " + "'%" + $scope.description + "%' ";
                sql = sql.concat(sqlapp);
            }
            else {
                first = false;
                var sqlapp = " WHERE message like " + "'%" + $scope.description + "%' ";
                sql = sql.concat(sqlapp);
            }
        }
        if ($scope.selectedDateFrom != undefined && $scope.selectedDateFrom != null && $scope.selectedDateFrom != '') {

            var hour = $scope.parseTime($scope.selectedHourFrom);

            var day = moment($scope.selectedDateFrom);
            var formatted = day.format("YYYY-MM-DD");
            if (!first) {
                var sqlapp = " and  date >= " + "'" + formatted + '' + hour + "'";
                sql = sql.concat(sqlapp);
            }
            else {
                first = false;
                var sqlapp = " WHERE date >= " + "'" + formatted + '' + hour + "'";
                sql = sql.concat(sqlapp);
            }
        }
        if ($scope.selectedDateTo != undefined && $scope.selectedDateTo != null && $scope.selectedDateTo != '') {
            var hour = $scope.parseTime($scope.selectedHourTo);
            var day = moment($scope.selectedDateTo);
            var formatted = day.format("YYYY-MM-DD");
            if (!first) {
                var sqlapp = " and  date <= " + "'" + formatted + '' + hour + "'";
                sql = sql.concat(sqlapp);
            }
            else {
                first = false;
                var sqlapp = " WHERE date <= " + "'" + formatted + '' + hour + "'";
                sql = sql.concat(sqlapp);
            }
        }
        console.log(sql)
        CommandLogApi.queryText({database: $routeParams.database, limit: -1, language: 'sql', text: sql }, function (data) {
            if (data) {
                $scope.headers = CommandLogApi.getPropertyTableFromResults(data.result);
                $scope.resultTotal = data;
                $scope.results = data.result.slice(0, $scope.countPage);
                $scope.currentPage = 1;
                $scope.numberOfPage = new Array(Math.ceil(data.result.length / $scope.countPage));
            }
        });
    }
    $scope.checkDateFrom = function () {
        if ($scope.selectedDateFrom == undefined || $scope.selectedDateFrom == '') {
            return true;
        }
        return false
    }
    $scope.checkDateTo = function () {
        if ($scope.selectedDateTo == undefined || $scope.selectedDateTo == '') {
            return true;
        }
        return false
    }
    $scope.$watch("countPage", function (data) {
        console.log($scope.resultTotal)
        if ($scope.resultTotal) {
            $scope.results = $scope.resultTotal.result.slice(0, $scope.countPage);
            $scope.currentPage = 1;
            $scope.numberOfPage = new Array(Math.ceil($scope.resultTotal.result.length / $scope.countPage));
        }
    });
    $scope.switchPage = function (index) {
        if (index != $scope.currentPage) {
            $scope.currentPage = index;
            $scope.results = $scope.resultTotal.result.slice(
                (index - 1) * $scope.countPage,
                index * $scope.countPage
            );
        }
    }
}
])
;