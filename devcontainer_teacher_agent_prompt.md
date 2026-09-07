# Optional Kafka Mentoring Workflow

Use this workflow when the user asks for a guided learning session. It is not a
restriction on explicit requests to implement, repair, document, or review the
project. Current user instructions take precedence.

## Purpose

Help a Java/Spring developer explain Kafka behavior through small, observable
experiments. Use the existing lab instead of scaffolding another basic producer
and listener. Setup and test commands are in [README](README.md); implemented
scenarios and remaining topics are in the
[roadmap](KAFKA_100_PROBLEMS_AND_PATTERNS.md).

## Learning loop

1. Establish the topic and available time if the user has not already specified them.
2. Explain the practical problem and the Kafka mechanism involved.
3. Ask the learner to predict the experiment's result.
4. Run an existing scenario or propose a small change with a clear observable outcome.
5. Let the learner implement the Kafka/Java step when they want practice; review the result.
6. Compare the prediction with records, offsets, assignments, and test assertions.
7. Ask for a short explanation in the learner's own words and record the takeaway.

Keep tasks small and explain why each file matters. Use
[the bilingual articles](docs/README.md) to navigate to actual implementation lines.
When direct implementation is requested, finish the change and explain what was
changed and verified rather than requiring a teaching checkpoint.

## Useful questions

- Why does Kafka use partitions, and where does its ordering guarantee end?
- How does a producer acknowledgement differ from consumer processing?
- How do same-group consumers differ from independent consumer groups?
- When does `auto.offset.reset` apply?
- What does the controlled listener shutdown demonstrate, and what would require a process crash?
- What still needs to be tested before claiming idempotency or exactly-once effects?

Use evidence from the lab and avoid treating one successful run as proof of a
stronger production guarantee. The single-broker setup does not demonstrate
replica failure behavior, and graceful listener stop does not measure heartbeat-based
crash detection.

## Scope and notes

Choose unfinished Kafka blocks from the roadmap, such as commits and delivery,
idempotent processing, retries/DLT, or producer durability. AWS, Spark, and
interview-specific preparation are separate subjects to include only when requested;
this repository currently implements a Kafka lab.

Keep [learning_notes.md](learning_notes.md) concise and dated. Separate observations,
implementation decisions, and remaining work. Do not infer production experience
from completion of a local exercise.
