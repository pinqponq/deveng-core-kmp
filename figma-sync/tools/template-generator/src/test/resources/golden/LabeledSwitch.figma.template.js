// url=https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=148-87

export default function template(figma) {
  const i = figma.selectedInstance;

  const labelValue = i.getString("label") || "...";
  const isCheckedValue = i.getBoolean("isChecked", { "true": true, "false": false });
  const isLabelAtStartValue = i.getBoolean("isLabelAtStart", { "true": true, "false": false });

  return `
LabeledSwitch(
    label = "${labelValue}",
    isChecked = ${isCheckedValue},
    isLabelAtStart = ${isLabelAtStartValue},
    onSwitchClick = { _ -> }
)
  `.trim();
}

export const metadata = { nestable: false };
