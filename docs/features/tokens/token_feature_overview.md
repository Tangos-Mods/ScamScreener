# Token Feature Overview

Token features are lexical pattern features generated from normalized message text. The extractor first builds a word sequence from lowercase alphanumeric tokens and then constructs contiguous n-grams. Each resulting phrase is stored as a feature key with a family prefix, such as `ng2:` for bigrams and `ng5:` for five-word phrases.

The model does not treat token features as counts. A token feature is either present or absent in the message being scored, and if present, its configured weight is added once to the linear score. This design keeps inference fast and stable while still allowing the model to learn phrase-level scam patterns that dense handcrafted features might miss.

In practice, token features complement dense context features. Dense features capture structural behavior and intent progression, while token features capture concrete language fragments that repeatedly correlate with scam outcomes in labeled data.
