// url=https://www.figma.com/design/sJoAsKB4qqqrwvHRlowppo/Design-System?node-id=150-63

export default function template(figma) {
  const i = figma.selectedInstance;

  const isEnabledValue = i.getBoolean("isEnabled", { "true": true, "false": false });
  const iconSwap = i.getInstanceSwap("icon");
  let iconValue = "Res.drawable.ic_placeholder";
  if (iconSwap && iconSwap.hasCodeConnect && iconSwap.hasCodeConnect()) {
    const result = iconSwap.executeTemplate();
    const drawable = result?.metadata?.props?.drawable;
    if (drawable) { iconValue = drawable; }
  }

  return `
CustomIconButton(
    isEnabled = ${isEnabledValue},
    icon = ${iconValue},
    onClick = { },
    iconDescription = ""
)
  `.trim();
}

export const metadata = { nestable: false };
