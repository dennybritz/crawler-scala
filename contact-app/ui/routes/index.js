var express = require("express");
var router = express.Router();
var stream = require("stream");
var amqp = require("amqp");
var log = require("morgan");
var uuid = require('node-uuid');

var rabbitURL = "amqp://guest:guest@localhost:5672";
var rabbitConnection = amqp.createConnection({url: rabbitURL});

router.get("/", function(req, res) {
  res.render("index");
});

router.get("/start", function(req, res) {
  res.render("start");
});

router.get("/data", function(req, res) {
  res.render("data");
});

router.post("/start", function(req, res) {
  var startUrl = req.body.url;
  var appUUID = uuid.v1();
  var appId = "com.blikk.contactapp." + appUUID;
  console.log("starting new app %s for %s", appId, startUrl);

  // Publish a request to the exchange
  rabbitConnection.exchange("", {passive: true}, function(ex){
    //console.log("exchange OK")
    var requestObj = {
      url: startUrl,
      appId: appId
    }
    ex.publish("com.blikk.contactapp.requests", JSON.stringify(requestObj));
  });
  res.send(appUUID);
});


router.get("/data/:appId", function(req, res) {
  var appId = "com.blikk.contactapp." + req.params.appId;
  var queueName = appId + "-out";
  
  // Create a new readable stream
  var rs = new stream.Readable();
  rs._read = function (){};

  // Subscribe to the rabbitMQ queue
  console.log("Reading from queue " + queueName);
  var q = rabbitConnection.queue(queueName, function (queue) {
    queue.subscribe(function (message, headers, deliveryInfo, messageObject) {
      var msgString = message.data.toString();
      var eventObj = JSON.parse(msgString);
      var eventStr = 
        "event: " + eventObj.eventType + "\n" +
        "data: " + eventObj.payload;
      console.log(eventObj);
      rs.push(eventStr + "\n\n");
    });
  });

  res.set("Content-Type", "text/event-stream");
  rs.pipe(res);
});

module.exports = router;
