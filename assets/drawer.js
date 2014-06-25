/*jslint bitwise: true */

// Bresenham's line algorithm
// http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm

window.Drawer = window.Drawer || {};

(function () {
    var repeatString = function (string, times) {
        var result = "";
        var i;
        for (i = 0; i < times; i += 1) {
            result += string;
        }
        return result;
    };

    var add = function (result, x, y, char) {
        var symbol = {v: char};
        var key = [x, y];
        result.points.push([[x, y], symbol]);
        result.index[x + "_" + y] = symbol;
    };

    var concat = function (result, other) {
        result.points = result.points.concat(other.points);
        var k;
        for (k in other.index) {
            if (other.index.hasOwnProperty(k)) {
                result.index[k] = other.index[k];
            }
        }
    };

    window.Drawer.buildLine = function (data, firstChar) {
        var x1 = data[0];
        var y1 = data[1];
        var x2 = data[2];
        var y2 = data[3];
        var dx = Math.abs(x2 - x1);
        var dy = Math.abs(y2 - y1);
        var sx = (x1 < x2 ? 1 : -1);
        var sy = (y1 < y2 ? 1 : -1);
        var err = dx - dy;
        var sym = (dx < dy ? "|" : "-");
        var slash = (sx === sy ? "\\" : "/");

        var result = {points: [], index: {}};
        while (x1 !== x2 || y1 !== y2) {
            var e2 = err << 1;
            var newErr;
            if (e2 > -dy && e2 < dx) {
                newErr = err - dy + dx;
            } else if (e2 > -dy) {
                newErr = err - dy;
            } else if (e2 < dx) {
                newErr = err + dx;
            } else {
                newErr = err;
            }
            var newX1 = (e2 > -dy ? x1 + sx : x1);
            var newY1 = (e2 < dx ? y1 + sy : y1);
            var newSym;
            if (result.points.length === 0 && firstChar) {
                newSym = firstChar;
            } else {
                newSym = ((sym === "|" && newX1 !== x1) || (sym === "-" && newY1 !== y1) ? slash : sym);
            }

            add(result, x1, y1, newSym);

            x1 = newX1;
            y1 = newY1;
            err = newErr;
        }
        add(result, x2, y2, (result.points.length === 0 && firstChar ? firstChar : sym));

        return result;
    };

    window.Drawer.buildRect = function (data) {
        var x1 = Math.min(data[0], data[2]);
        var y1 = Math.min(data[1], data[3]);
        var x2 = Math.max(data[0], data[2]);
        var y2 = Math.max(data[1], data[3]);
        var result = {points: [], index: {}};
        add(result, x1, y1, "+");
        if (x1 !== x2) {
            add(result, x2, y1, "+");
        }
        if (y1 !== y2) {
            add(result, x1, y2, "+");
        }
        if (x1 !== x2 && y1 !== y2) {
            add(result, x2, y2, "+");
        }
        if (Math.abs(x2 - x1) > 1) {
            concat(result, window.Drawer.buildLine([x1 + 1, y1, x2 - 1, y1], "-"));
            if (Math.abs(y2 - y1) > 0) {
                concat(result, window.Drawer.buildLine([x1 + 1, y2, x2 - 1, y2], "-"));
            }
        }
        if (Math.abs(y2 - y1) > 1) {
            concat(result, window.Drawer.buildLine([x1, y1 + 1, x1, y2 - 1], "|"));
            if (Math.abs(x2 - x1) > 0) {
                concat(result, window.Drawer.buildLine([x2, y1 + 1, x2, y2 - 1], "|"));
            }
        }
        return result;
    };

    window.Drawer.buildRectLine = function (data, direction) {
        var x1 = data[0];
        var y1 = data[1];
        var x2 = data[2];
        var y2 = data[3];
        direction = direction || "horizontal";

        var result = {points: [], index: {}};
        if (direction === "horizontal") {
            add(result, x2, y1, "+");
            if (Math.abs(x2 - x1) > 0) {
                concat(result, window.Drawer.buildLine([x1, y1, x2 > x1 ? (x2 - 1) : (x2 + 1), y1], "-"));
            }
            if (Math.abs(y2 - y1) > 0) {
                concat(result, window.Drawer.buildLine([x2, y2 > y1 ? (y1 + 1) : (y1 - 1), x2, y2], "|"));
            }
        } else {
            add(result, x1, y2, "+");
            if (Math.abs(y2 - y1) > 0) {
                concat(result, window.Drawer.buildLine([x1, y1, x1, y2 > y1 ? (y2 - 1) : (y2 + 1)], "|"));
            }
            if (Math.abs(x2 - x1) > 0) {
                concat(result, window.Drawer.buildLine([x2 > x1 ? (x1 + 1) : (x1 - 1), y2, x2, y2], "-"));
            }
        }
        return result;
    };

    window.Drawer.sortData = function (data) {
        return data.sort(function (a, b) {
            var result;
            if (a[0][1] > b[0][1]) {
                result = 1;
            } else if (a[0][1] < b[0][1]) {
                result = -1;
            } else {
                if (a[0][0] > b[0][0]) {
                    result = 1;
                } else if (a[0][0] < b[0][0]) {
                    result = -1;
                } else {
                    result = 0;
                }
            }
            return result;
        });
    };

    window.Drawer.generateData = function (width, height, points) {
        var line = 0;
        var pos = 0;
        var result = "";
        var i;
        for (i in points) {
            if (points.hasOwnProperty(i)) {
                var dataPoint = points[i];
                var point = dataPoint[0];
                var x = point[0];
                var y = point[1];
                var char = dataPoint[1];
                while (y !== line) {
                    result += repeatString(" ", width - pos);
                    result += "\n";
                    pos = 0;
                    line += 1;
                }
                result += repeatString(" ", x - pos);
                result += char.v;
                pos = x + 1;
                line = y;
            }
        }
        while (line < height) {
            result += repeatString(" ", width - pos);
            pos = 0;
            line += 1;
            if (line < height) {
                result += "\n";
            }
        }
        return result;
    };
}());
