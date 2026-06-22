# Demo Script – FraudShield AI

## Scenario: Operation Suraksha Walkthrough

**Setup**: Master scammer number `+918765432109` is seeded in Neo4j (run with `--spring.profiles.active=seed`)

### Step 1 – Fraud Shield Text Analysis
1. Type in the Fraud Shield input:
   > "A man claiming to be from the CBI called me and said my Aadhaar is linked to a money laundering case in Mumbai. He asked me to stay on a video call for digital arrest verification."
2. Hit Analyze → should return DIGITAL_ARREST with 90%+ confidence
3. Point out the trigger phrases extracted (Aadhaar, digital arrest, CBI, video call)

### Step 2 – Graph Intelligence Connection
1. Navigate to Graph Intelligence tab
2. Search for `+918765432109`
3. Watch the fraud ring light up — 4 scammer nodes + 6 victim nodes + 3 bank accounts
4. Click the master scammer node → show "CRITICAL risk, 12 complaints, Jamtara Jharkhand"
5. Point out the 4 unreported victims (reported = false) that the system discovered

### Key Quote for Judges
> "This number is already connected to 6 victims in our network. Without this system, each victim's complaint would have been investigated in isolation. FraudShield found the unreported victims BEFORE they came forward."

## Test Cases (Run all 25 on Day 5)
See `test_cases.md` for the full list.
