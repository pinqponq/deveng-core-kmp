#!/usr/bin/env node
/**
 * Guardrail: Enforces Template V2 as the only Code Connect input source.
 *
 * This script validates that figma.config.json is configured for Template V2 mode,
 * which uses generated `.figma.template.js` files instead of the legacy Compose
 * parser mode (which parsed `*Doc.kt` Kotlin files directly).
 *
 * Validation rules:
 * - codeConnect.include MUST point to `.figma.template.js` files
 * - codeConnect.include MUST NOT target Kotlin files (*.kt, *Doc.kt)
 * - codeConnect.parser MUST NOT be set (setting it enables legacy Compose mode)
 * - codeConnect.exclude MUST NOT exclude template files
 * - Only one figma.config.json should exist (at repo root)
 */

const fs = require("fs");
const path = require("path");

const rootDir = path.resolve(__dirname, "../..");
const rootConfigPath = path.join(rootDir, "figma.config.json");
const blockedDirs = new Set([
  ".git",
  ".gradle",
  ".idea",
  ".kotlin",
  ".kotlin-js-store",
  "build",
  "node_modules",
  "tmp",
]);

const failures = [];

function collectConfigPaths() {
  const stack = [rootDir];
  const configs = [];

  while (stack.length) {
    const current = stack.pop();
    let entries;
    try {
      entries = fs.readdirSync(current, { withFileTypes: true });
    } catch {
      continue;
    }

    for (const entry of entries) {
      if (blockedDirs.has(entry.name)) continue;
      const fullPath = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(fullPath);
        continue;
      }
      if (entry.isFile() && entry.name === "figma.config.json") {
        configs.push(fullPath);
      }
    }
  }

  return configs;
}

function hasCanonicalPointer(filePath) {
  try {
    const text = fs.readFileSync(filePath, "utf8").toLowerCase();
    return (
      text.includes("root figma.config.json is canonical") ||
      text.includes("use root figma.config.json only")
    );
  } catch {
    return false;
  }
}

function validateSingleConfig(configPaths) {
  if (configPaths.length === 0) {
    failures.push("Missing figma.config.json at repo root.");
    return;
  }

  const extras = configPaths.filter((p) => path.resolve(p) !== rootConfigPath);
  const offenders = extras.filter((p) => !hasCanonicalPointer(p));

  if (offenders.length > 0) {
    failures.push(
      `Multiple figma.config.json files found: ${offenders.join(
        ", "
      )}. Delete duplicates or replace them with a pointer file stating the root config is canonical.`
    );
  }
}

function loadRootConfig() {
  if (!fs.existsSync(rootConfigPath)) {
    failures.push("Root figma.config.json is missing.");
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(rootConfigPath, "utf8"));
  } catch (err) {
    failures.push(`Failed to parse ${rootConfigPath}: ${err.message}`);
    return null;
  }
}

function validateCodeConnect(config) {
  const cc = config?.codeConnect;
  if (!cc) {
    failures.push(`${rootConfigPath} is missing the codeConnect block.`);
    return;
  }

  if (cc.parser) {
    failures.push(
      `${rootConfigPath} has codeConnect.parser set to "${cc.parser}". Remove it to avoid Compose parsing.`
    );
  }

  const includeGlobs = Array.isArray(cc.include) ? cc.include : [];
  const excludeGlobs = Array.isArray(cc.exclude) ? cc.exclude : [];

  const kotlinGlobs = includeGlobs.filter(
    (glob) => glob.includes("Doc.kt") || glob.includes(".kt")
  );
  if (kotlinGlobs.length > 0) {
    failures.push(
      `${rootConfigPath} include globs must not target Kotlin. Offending entries: ${kotlinGlobs.join(
        ", "
      )}`
    );
  }

  if (!includeGlobs.some((glob) => glob.includes(".figma.template.js"))) {
    failures.push(
      `${rootConfigPath} must include Template V2 files (e.g., figma-sync/templates/**/*.figma.template.js).`
    );
  }

  const templateExclusions = excludeGlobs.filter((glob) =>
    glob.includes(".figma.template.js")
  );
  if (templateExclusions.length > 0) {
    failures.push(
      `${rootConfigPath} excludes template files: ${templateExclusions.join(
        ", "
      )}. Remove template exclusions.`
    );
  }
}

function main() {
  const configPaths = collectConfigPaths();
  validateSingleConfig(configPaths);

  const rootConfig = loadRootConfig();
  if (rootConfig) {
    validateCodeConnect(rootConfig);
  }

  if (failures.length > 0) {
    console.error("verifyCodeConnectInputs failed:");
    for (const failure of failures) {
      console.error(`- ${failure}`);
    }
    process.exit(1);
  }

  console.log("verifyCodeConnectInputs passed: template-only inputs are enforced.");
}

main();
