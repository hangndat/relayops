import nextVitals from "eslint-config-next/core-web-vitals";

const eslintConfig = [
  ...nextVitals,
  {
    rules: {
      // Operator console pages intentionally sync remote API state after key/filter changes.
      "react-hooks/set-state-in-effect": "off",
    },
  },
];

export default eslintConfig;
