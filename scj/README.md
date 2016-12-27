# scj - SCI (Simple Console Interface) for Jem
Console tool for processing e-books.

## Required
* jem-core
* jem-epm
* jem-formats
* qaf-cli

## Command line examples
Convert UMD book to ePub and set new title, author

    java -jar scj-1.0.jar input.umd -c -t epub -o output.epub -atitle=Example -a author=PW

View attributes of UMD book and its chapter

    java -jar scj-1.0.jar input.umd -w title -w publisher -w chapter1$title

View table of contents (TOC) of ePub book

    java -java scj-1.0.jar input.epub -w toc

More example see the doc of scj.
