function storePwd() {
    localStorage.pwd = document.getElementById('pwd').value;
    console.log("Pwd updated to " + localStorage.pwd);
}

function indexInit() {
    hide(".shuntedit");
    show(".shuntview")

    if( !localStorage.pwd )
        localStorage.pwd = "";
    document.getElementById("pwd").value = localStorage.pwd;

	var source = new EventSource("../events");
	addTempListener(source, "outdoor");
	addTempListener(source, "indoor");
	addTempListener(source, "hotwater");
	addTempListener(source, "coldwater");
	addTempListener(source, "tankwater");
	source.addEventListener("skruv", function(e) {
		if( e.data == "LOW" ) {
		    document.getElementById("panna_on").style.display='none';
		    document.getElementById("panna_off").style.display='inline';
		} else {
            document.getElementById("panna_on").style.display='inline';
            document.getElementById("panna_off").style.display='none';
        }
	}, false);
	source.addEventListener("shuntlevel", function(e) {
	    var v = e.data;
	    document.getElementById("shuntlevel").innerHTML = v;
	    var elmArr = document.querySelectorAll("option")
        var i;
        for(i=0; i<elmArr.length; i++) {
            elmArr[i].removeAttribute("selected");
            if( elmArr[i].value == v )
                elmArr[i].setAttribute("selected", "selected");
        }
	}, false);
	var r = new XMLHttpRequest();
	r.open("GET", "../graphdata", true);
	r.onreadystatechange = function() {
		  if (r.readyState == 4 && r.status == 200) {
		    setupGrapth(r.responseText);
		  }
		};
	r.send();
}

function shuntChanged() {
    var value = document.querySelector("#shuntselector").value;
    console.log("Changed to " + value);
    var pwd = document.getElementById("pwd").value;
    var r = new XMLHttpRequest();
    r.onreadystatechange = function() {
        if (r.readyState == XMLHttpRequest.DONE && r.status != 200) {
            alert(r.responseText);
        }
    }
    r.open("POST", "../shunt", true);
    r.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    var data = "pwd="+pwd + "&value=" + value;
    r.send(data);

    hide(".shuntedit");
    show(".shuntview")
}

function addTempListener(source, id) {
	source.addEventListener(id, function(e) {
		var html = document.getElementById(id);
		html.innerHTML = e.data;
	}, false);
}

function hide(selector) {
    var elmArr = document.querySelectorAll(selector);
    var i;
    for(i=0; i<elmArr.length; i++) {
        elmArr[i].style.display = 'none';
    }
}

function show(selector) {
    var elmArr = document.querySelectorAll(selector);
    var i;
    for(i=0; i<elmArr.length; i++) {
        elmArr[i].style.display = 'table-row';
    }
}

function showShuntEdit() {
    show(".shuntedit");
    hide(".shuntview")
}

function shunt(direction) {
	var pwd = document.getElementById("pwd").value;
	var r = new XMLHttpRequest();
	r.onreadystatechange = function() {
        if (r.readyState == XMLHttpRequest.DONE && r.status != 200) {
            alert(r.responseText);
        }
    }
	r.open("GET", "../shunt/"+direction+"?pwd="+pwd, true);
	r.send();
}

function setupGrapth(data) {
	data = JSON.parse(data);
	var outdoorDat = data.outdoor;
	var hotwaterDat = data.hotwater;
	var coldwaterDat = data.coldwater;
	var tankwaterDat = data.tankwater;
	var indoorDat = data.indoor;
//	var skruvDat = new Array();
	//var skruv = data.skruv;
	//for(var i=0;i<skruv.length;i++){
//		var a = new Array();
//		a.push(skruv[i][0]);
//		a.push(skruv[i][1]/36);
//		skruvDat.push(a);
//	}
	var skruvDat = data.skruv;

	$('#graph').highcharts({
	    chart: {
	        zoomType: 'xy'
	    },
	    title: {
	        text: ''
	    },
	    subtitle: {
	        text: ''
	    },
	    
	    xAxis: [{
	     			type: 'datetime',
		            dateTimeLabelFormats: { // don't display the dummy year
		                month: '%e. %b',
		                year: '%b'
		            },
		            title: {
		                text: 'Tid och datum'
		            }
	    }],
	    
	    yAxis: [{ // Primary yAxis
	        labels: {
	            format: '{value} minuter',
	            style: { color: Highcharts.getOptions().colors[0] }
	        },
	        title: {
	            text: 'Pellets',
	            style: {
	                color: Highcharts.getOptions().colors[0]
	            }
	        }
	
	    }, { // Secondary yAxis
	        gridLineWidth: 0,
	        max: 80,
	        title: {
	            text: '',//Utomhus',
	            style: { color: Highcharts.getOptions().colors[1] }
	        },
	        labels: {
	            format: '{value} °C',
	            style: {
	                color: Highcharts.getOptions().colors[1]
	            }
	        },
	        opposite: true
	
	    }, { // Tertiary yAxis
	        gridLineWidth: 0,
	        max: 80,
	        title: {
	            text: '',//Inomhus',
	            style: { color: Highcharts.getOptions().colors[2] }
	        },
	        labels: {
	            format: '{value} °C',
	            style: {
	                color: Highcharts.getOptions().colors[2]
	            }
	        },
	        opposite: true
	    	
	    }, { // 4th yAxis
	        gridLineWidth: 0,
	        max: 80,
	        title: {
	            text: '',//Från pannan',
	            style: { color: Highcharts.getOptions().colors[2] }
	        },
	        labels: {
	            format: '{value} °C',
	            style: {
	                color: Highcharts.getOptions().colors[2]
	            }
	        },
	        opposite: true
	    	
	    }, { // 5th yAxis
	        gridLineWidth: 0,
	        max: 80,
	        title: {
	            text: '',//Tillbaka till pannan',
	            style: { color: Highcharts.getOptions().colors[2] }
	        },
	        labels: {
	            format: '{value} °C',
	            style: {
	                color: Highcharts.getOptions().colors[2]
	            }
	        },
	        opposite: true
	    }],
	    tooltip: {
	        shared: true
	    },
	    series: [{
	        name: 'Pellets',
	        type: 'column',
	        yAxis: 0,
	        data: skruvDat,
	        marker: { enabled: false },
	        tooltip: { valueSuffix: ' minuter per timme' }
	    }, {
	        name: 'Utomhus',
	        type: 'spline',
	        yAxis: 1,
	        data: outdoorDat,
	        marker: {enabled: false},
	        tooltip: { valueSuffix: ' °C' }
	    }, {
	        name: 'Inomhus',
	        type: 'spline',
	        yAxis: 1,
	        data: indoorDat,
	        marker: { enabled: false },
	        tooltip: { valueSuffix: ' °C' }
	    }, {
	        name: 'Panntemperatur',
	        type: 'spline',
	        yAxis: 1,
	        data: tankwaterDat,
	        marker: { enabled: false },
	        tooltip: { valueSuffix: ' °C' }
	    }, {
	        name: 'Radiator ut',
	        type: 'spline',
	        yAxis: 1,
	        data: hotwaterDat,
	        marker: { enabled: false },
	        tooltip: { valueSuffix: ' °C' }
	    }, {
	        name: 'Radiator retur',
	        type: 'spline',
	        yAxis: 1,
	        data: coldwaterDat,
	        marker: { enabled: false },
	        tooltip: { valueSuffix: ' °C' }
	    }]
	});
}

