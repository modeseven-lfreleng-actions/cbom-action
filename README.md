<!--
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 The Linux Foundation
-->

# 🔑 CBOM Generator

<!-- prettier-ignore-start -->
<!-- markdownlint-disable-next-line MD013 -->
[![Linux Foundation](https://img.shields.io/badge/Linux-Foundation-blue)](https://linuxfoundation.org/) [![Source Code](https://img.shields.io/badge/GitHub-100000?logo=github&logoColor=white&color=blue)](https://github.com/lfreleng-actions/cbom-action) [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![pre-commit.ci status badge]][pre-commit.ci results page] [![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/lfreleng-actions/cbom-action/badge)](https://scorecard.dev/viewer/?uri=github.com/lfreleng-actions/cbom-action)
<!-- prettier-ignore-end -->

Generates a CycloneDX Cryptography Bill of Materials (CBOM) describing the
cryptographic assets a project uses: algorithms, key sizes, modes, protocols
and certificates.

A CBOM answers "which cryptography does this code actually call?", which an
SBOM cannot, and it feeds post-quantum readiness assessments. This action is
the CBOM counterpart to
[sbom-action](https://github.com/lfreleng-actions/sbom-action) and mirrors
its input and output contract.

## cbom-action

## Usage Example

<!-- markdownlint-disable MD046 -->

```yaml
steps:
  - name: "Generate CBOM"
    id: cbom
    uses: lfreleng-actions/cbom-action@main
    with:
      path_prefix: "."

  - name: "Upload CBOM"
    if: steps.cbom.outputs.file_count != '0'
    uses: actions/upload-artifact@v7
    with:
      name: "cbom-files"
      path: ${{ steps.cbom.outputs.cbom_pattern }}
```

<!-- markdownlint-enable MD046 -->

## Inputs

<!-- markdownlint-disable MD013 -->

| Name             | Required | Default            | Description                                                         |
| ---------------- | -------- | ------------------ | ------------------------------------------------------------------- |
| path_prefix      | False    | `.`                | Project directory; must resolve within the workspace                |
| languages        | False    | (auto-detect)      | Comma-separated languages to scan: `java`, `python`, `go`, `csharp` |
| output_directory | False    | `.`                | CBOM report directory, within workspace or runner temp              |
| exclude          | False    | (upstream default) | Comma-separated Java regex patterns excluded from scanning          |
| module_cboms     | False    | `true`             | Emit a per-module CBOM alongside the consolidated one               |
| empty_cboms      | False    | `true`             | Write CBOM files even when a scan finds no assets                   |
| fail_on_error    | False    | `false`            | Fail the action if CBOM generation encounters errors                |
| image            | False    | (pinned digest)    | CBOMkit scanner image; pin by digest to keep results reproducible   |

<!-- markdownlint-enable MD013 -->

## Outputs

<!-- markdownlint-disable MD013 -->

| Name         | Description                                                            |
| ------------ | ---------------------------------------------------------------------- |
| cbom_path    | Path to the consolidated CBOM, empty when none produced                |
| cbom_pattern | Glob matching every CBOM file produced                                 |
| file_count   | Number of CBOM files produced                                          |
| asset_count  | Cryptographic assets in the consolidated CBOM                          |
| languages    | Languages actually scanned                                             |
| outcome      | `success`, `failed`, or `skipped` (no supported language present)      |

<!-- markdownlint-enable MD013 -->

## Supported Languages

Detection comes from the
[sonar-cryptography](https://github.com/cbomkit/sonar-cryptography) plugin,
and covers specific cryptographic libraries rather than whole languages:

<!-- markdownlint-disable MD013 -->

| Language | Library                                | Coverage                                     |
| -------- | -------------------------------------- | -------------------------------------------- |
| Java     | JCA                                    | Full                                         |
| Java     | BouncyCastle (light-weight API)        | Full                                         |
| Python   | `pyca/cryptography`                    | Full                                         |
| Go       | `crypto` standard library              | Full, except `crypto/x509`                   |
| Go       | `golang.org/x/crypto`                  | Partial: `hkdf`, `pbkdf2`, `sha3`            |
| C#       | `System.Security.Cryptography`         | Preview; **not production ready**            |

<!-- markdownlint-enable MD013 -->

Passing `csharp` emits a warning. Upstream ships it with no detection rules
beyond those used to verify the engine, no cross-method variable tracking,
and C# v7 syntax support alone.

Auto-detection never selects `csharp`. It picks `java` from `pom.xml`,
`build.gradle` or `build.gradle.kts`; `go` from `go.mod`; and `python` from
`pyproject.toml`, `setup.py`, `setup.cfg`, `requirements*.txt` or any `.py`
file. When it finds no supported language the action exits with
`outcome: skipped` rather than failing.

## Implementation Details

### Runner requirements

The action needs `docker`, `jq`, and GNU `realpath` with `-m` support on the
runner, and checks for all three up front so a missing tool reports itself
rather than surfacing as a confusing mid-run failure. GitHub-hosted Ubuntu
runners carry them as standard; minimal self-hosted images and macOS runners
(whose BSD `realpath` lacks `-m`) may not.

### Container invocation

The scanning engine is [CBOMkit](https://github.com/cbomkit), invoked as a
pinned container rather than by composing the upstream
`cbomkit/cbomkit-action`. Three reasons:

**Determinism.** The `image` default names a digest. Upstream releases up to
and including v2.2.0 shipped `image: 'Dockerfile'` with a
`FROM ghcr.io/cbomkit/cbomkit-action:edge` base, so pinning that action to a
commit SHA still resolved to a floating image rebuilt from `main`. Upstream
fixed this in v2.3.0, but its release job force-moves git tags, so a SHA pin
names a commit that might not stay reachable.

**`path_prefix`.** The scanner reads `GITHUB_WORKSPACE` and offers no
project-directory option, so it always scans the entire workspace. Invoking
the container directly lets the project directory mount *as* the workspace.

**Output ownership.** The image declares `USER 0:0`, so CBOMs would land in
the workspace owned by root and break later workflow steps. This action runs
the container with an explicit `--user`.

### Java accuracy

Java scanning resolves symbols from compiled classes and dependency jars, so
a prior build yields the best accuracy. A scan of an unbuilt tree still
succeeds; it resolves fewer symbols. Building the project in an earlier step
of the same job gives the strongest results.

This action does not expose the upstream `CBOMKIT_JAVA_REQUIRE_BUILD`
setting, which upstream documents as failing a Java scan of an unbuilt tree.
That setting cannot take effect in the published image: upstream guards it
on `javaDependencyJars.isEmpty() && javaClassDirectories.isEmpty()`, but
`Main.java` unconditionally adds the project directory to both lists, so the
guard never fires. The action pins the value to `false` so that a future
upstream fix cannot start failing jobs that callers asked to keep
non-blocking.

### Network access

The action pulls a container image, so a workflow running under
`step-security/harden-runner` with a blocking egress policy must permit
`ghcr.io` and `pkg-containers.githubusercontent.com`.

## Notes

`fail_on_error` defaults to `false`, which differs from `sbom-action`. CBOM
output is advisory and gates nothing downstream, so a scanner problem should
surface as a warning rather than break an unrelated pipeline. Set it to
`true` where a missing CBOM must count as a build failure.

[pre-commit.ci results page]: https://results.pre-commit.ci/latest/github/lfreleng-actions/cbom-action/main
[pre-commit.ci status badge]: https://results.pre-commit.ci/badge/github/lfreleng-actions/cbom-action/main.svg
