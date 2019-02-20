
# Release Notes

## 1.1.2 - 2019-02-20
* Fix bug where using a VCF input that has no variant lines would output a blank VCF (instead of a VCF with just the header lines)

## 1.1.1 - 2019-02-12
* When any pipe fails (bior_* commands piped together), fail the whole command instead of eating the error and exiting with exitcode 0
* Fix name of command in the command help
* Enlarge scan of header to include 20,000 lines instead of 2,000 (since some headers are bigger than that)
* Reset the IFS variable after the running the commands so tabs and newlines don't get shredded
* Check the exit code of the full bior_* command pipe and error on any problems
* Account for VCFs that have no variant lines (instead of failing)


## 1.1.0 - 2018-07-25
* Added cutSamples groovy code to remove samples columns from VCF before processing
* Added merge groovy code to merge annotated VCF output files back into a single VCF
* Small fixes to bior_annotate_lite


## 1.0.0 - 2018-05-10
* Initial release

