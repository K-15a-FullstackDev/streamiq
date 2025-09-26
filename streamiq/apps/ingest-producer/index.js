const { Kafka, logLevel } = require("kafkajs");
const { v4: uuidv4 } = require("uuid");
require("dotenv").config();

const brokers = process.env.KAFKA_BROKERS.split(",");
const rawTopic = process.env.TOPIC || "raw.events.v1";
const classifiedTopic = process.env.CLASSIFIED_TOPIC || "classified.events.v1";
const classifierUrl =
  process.env.CLASSIFIER_URL || "http://localhost:8000/classify";

const kafka = new Kafka({
  clientId: "ingest-producer",
  brokers,
  logLevel: logLevel.INFO,
});
const producer = kafka.producer();

function randomEvent() {
  const isSensor = Math.random() < 0.5;
  if (isSensor) {
    return {
      event_id: uuidv4(),
      ts: new Date().toISOString(),
      source: "sensor",
      payload: {
        device: `edge-${Math.floor(Math.random() * 50)}`,
        temp: +(20 + Math.random() * 15).toFixed(2),
        humidity: +(30 + Math.random() * 50).toFixed(2),
      },
    };
  } else {
    const samples = [
      "Service degraded at Dallas edge",
      "User reports login failure",
      "CPU spike detected",
      "Cache miss storm observed",
      "Slow query on orders table",
      "All systems stable",
      "Incident resolved",
    ];
    return {
      event_id: uuidv4(),
      ts: new Date().toISOString(),
      source: "twitter",
      payload: {
        text: samples[Math.floor(Math.random() * samples.length)],
        user: `u${Math.floor(Math.random() * 10000)}`,
      },
    };
  }
}

async function classify(ev) {
  try {
    const res = await fetch(classifierUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(ev),
    });
    if (!res.ok) throw new Error(`Classifier ${res.status}`);
    return await res.json();
  } catch (e) {
    console.error("Classifier error:", e.message);
    // fall back to unknown label
    return {
      event_id: ev.event_id,
      ts: ev.ts,
      source: ev.source,
      classification: { label: "unknown", confidence: 0.0, reasons: {} },
    };
  }
}

(async () => {
  await producer.connect();
  console.log(
    `Producing raw -> ${rawTopic}, classified -> ${classifiedTopic} via ${brokers.join(
      ","
    )}`
  );

  let i = 0;
  setInterval(async () => {
    const ev = randomEvent();
    try {
      // 1) Always send raw
      await producer.send({
        topic: rawTopic,
        messages: [{ key: ev.source, value: JSON.stringify(ev) }],
      });

      // 2) Classify and send classified
      const labeled = await classify(ev);
      const out = { ...ev, classification: labeled.classification };
      await producer.send({
        topic: classifiedTopic,
        messages: [{ key: ev.source, value: JSON.stringify(out) }],
      });

      i++;
      if (i % 10 === 0) console.log(`Sent ${i} events (raw + classified)`);
    } catch (err) {
      console.error("Send error:", err);
    }
  }, 750);
})();
