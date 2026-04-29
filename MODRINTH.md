# ScamScreener 2.3.4

This patch fixes sender-backed chat classification and avoids repeating the same regex work across rule checks.

- Chat callbacks that only expose the sender through Fabric message params now still enter the pipeline as player chat.
- Rule and funnel regex checks now reuse cached per-message matches instead of running the same pattern repeatedly.
