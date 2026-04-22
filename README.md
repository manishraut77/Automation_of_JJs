# Automation_of_JJs

This repo now contains a **Maven + JavaFX** front-end implementation for the **Waiter** and **Busboy** modules.

## Run

From the repo root:

```bash
mvn javafx:run
```

## Demo accounts

- **Waiter**: username `waiter1`, password `a1b2c3`
- **Busboy**: username `busboy1`, password `b1c2d3`

You can also create new accounts using **Sign Up** (Employee ID must be 6 alphanumeric chars; password must be exactly 6 chars and not uniform/sequential).

## Notes

- The original Swing prototype files (`JJMain.java`, etc.) are still in the repo root for reference, but the runnable JavaFX app lives under `src/main/java`.