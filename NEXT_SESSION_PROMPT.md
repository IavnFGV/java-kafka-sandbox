# Next Session Prompt

Use this as an optional starting prompt for a learning session. Repository status
belongs in the linked documents rather than a second copy of the scenario list.
Explicit instructions in the current session take precedence over this template.

```text
We are continuing the Java Kafka Sandbox project.

First inspect the current branch and working tree, then read:
- README.md for setup, commands, implemented scope, and limitations
- VISUALIZER_CONTEXT.md for architecture and execution flow
- KAFKA_100_PROBLEMS_AND_PATTERNS.md for implemented mappings and remaining work
- docs/README.md for Russian/English scenario articles and implementation links
- learning_notes.md for historical decisions and takeaways

Scenarios 001–012 are implemented. Select the next unfinished block from the
roadmap, or follow the task I give you; do not rebuild an already completed
scenario merely because an older diary entry calls it the next step.

For a learning task:
- Explain the practical problem and what we want to observe before asking me to code.
- Give me 1–3 concrete steps, then review my implementation and explanations.
- Prefer small Kafka experiments with meaningful verification over broad scaffolding.
- UI/documentation work may be implemented directly when it supports the exercise.
- If I explicitly ask you to implement a change, carry it through to completion.

When completing a scenario:
- Derive visualization from actual experiment results and state its limitations.
- Keep catalog backlog IDs and the roadmap synchronized; distinguish partial
  coverage from a completed topic.
- Add or update both .ru.md and .en.md articles with valid source line links.
- Explain the important Spring/Java techniques as well as the Kafka behavior.
- Add a concise dated learning note and update the setup/architecture docs if needed.
- Run checks appropriate to the change; Kafka integration tests need the broker.
- Keep commits focused when committing is requested or appropriate to the task.
```

For more detail on the mentoring style, see the
[teaching workflow](devcontainer_teacher_agent_prompt.md).
