---
inclusion: manual
---

# Deployment Notes

## Release Sequence for New Features or Breaking Changes

1. Update the main package in a PR.
2. After merging the PR, release a version to Maven Central with a `-pre` suffix.
3. Update the sample projects with the new features and changes, targeting the `-pre` package, in a PR.
4. Once the `-pre` package has been verified using the new sample projects, create another release without the `-pre` suffix, and create another PR to retarget the sample projects to the non `-pre` suffixed package.
