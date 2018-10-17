Griffons
========

Introduction
------------

Griffons are program that are able to apply conversion, transformation or identification on binary files.

Griffon, batch and inner process
--------------------------------

- An inner process is specific execution to apply on a file; like `magick convert -auto-orient test.jpg test.jpg`. Here we want to rotate a picture `test.jpg`.
- A batch is a collection of process or command to execute (like the previous one).
- A griffon is a execution of one or several batches.

Status
------

-  `OK` with exit code 0. It means that every thing goes well.
-  `ERROR` with exit code 1. It means that the griffons, the batch or the inner process stop abruptly.
-  `WARNING` with exit code 2. It means that something goes wrong on the griffons, batch or the inner process.

Action
------

- `ANALYZE` like check if a picture is corrupted or not.
- `IDENTIFY` like identify the file, and find it's PUID.
- `GENERATE` like generate a GIF from a JPG picture.
- `EXTRACT` like extract some EXIF data from a picture.

Analyze result
--------------

- `VALID_ALL` means that the file is the right format and is not corrupted.
- `NOT_VALID` means that the file is the right format but may be corrupted.
- `WRONG_FORMAT` means that the file is in the wrong format.

Parameters
----------

Parameters are the required variables in order to run a batch in agriffon serialized in a JSON file named `parameters.json`:
- `requestId` of type string, is the vitam request id.
- `id` of typestring, is the batch id.
- `debug` og type boolean, show or not the following thing in `result.json` file: `result`, `error`,`executed`.
- `actions` of type list of object, is the list of action to do on an input. An action is compose of the following:
    * `type` of type string, is one of the following: `ANALYZE`,`IDENTIFY`, `GENERATE`, `EXTRACT`.
    * `values` of type object, containing the necessary values to run the action.
- `inputs` of type list of object, is the list of file to process, an input is compose of:
    * `name` of type string, is the name of the input.
    * `formatId` of type string, is the puid of the input.

Result
------

Result of the griffon execution is set in a JSON file named `result.json` with the following information:
- `requestId` of type string, is the vitam request id.
- `id` of type string, is the batch id.
- `outputs` of type list of object, representing the list of results. The output is compose withe the following variables:
    * `result` (show only in `debug` mode) of type string, is the log result of th execution (for example the `stdout` of a command).
    * `error` (show only in `debug` mode) of type string, is the log error of th execution (for example the `stderr` of a command).
    * `executed` (show only in `debug` mode) of type string, is what thing has been executed (for example a command and it’s arguments).
    * `input` of type input, is the input received.
    * `outputName` of type string, is the name of the output file, result of the action.
    * `status` of type string, is the status of the execution, one of `OK`, `ERROR`, `WARNING`
    * `analyseResult` (show only in `ANALYZE`) of type string, one of `VALID_ALL`, `NOT_VALID`, `WRONG_FORMAT`.
    * `action` of type string, is the action executed, one of `ANALYZE`, `IDENTIFY`, `GENERATE`, `EXTRACT`.

Input files
-----------

Input files are set in a directory named `input-files`.

Output files
------------

Output files are set in a directory named `output-files`.

Ready to process
----------------

To show that the batch is ready to process, we have a file named `<BATCH_ID>.ready` where `<BATCH_ID>` is the id of the batch.

Process done
------------

To show that the batch is done to process, we have a file named `<BATCH_ID>.done` where `<BATCH_ID>` is the id of the batch.

Example
-------

Here `f5a5f253-04a5-4567-b88a-c5af7df633df` is the batch id.
- `./imagemagick-griffon-vitam/f5a5f253-04a5-4567-b88a-c5af7df633df.ready` is the file showing that the griffon is able to process the batch.
- `./imagemagick-griffon-vitam/f5a5f253-04a5-4567-b88a-c5af7df633df/parameters.json` is the file where all parameters are set.
- `./imagemagick-griffon-vitam/f5a5f253-04a5-4567-b88a-c5af7df633df/result.json` is the file where processing results are set.
- `./imagemagick-griffon-vitam/f5a5f253-04a5-4567-b88a-c5af7df633df/input-files/` is the directory where files to process are set.
- `./imagemagick-griffon-vitam/f5a5f253-04a5-4567-b88a-c5af7df633df/output-files/` is the directory where processed files are set.
- `./imagemagick-griffon-vitam/f5a5f253-04a5-4567-b88a-c5af7df633df.done` is the file to set when it is done.

`parameters.json` example
~~~~~~~~~~~~~~~~~~~~~~~~~

.. code:: json
{
  "requestId": "4f6ae8d7-cab7-4f8d-b5e1-d5c0a1ea5793",
  "id": "1479591e-d325-456f-8409-697f3a757bf7",
  "debug": false,
  "actions": [
    {"type": "GENERATE", "values": {"extension": "GIF", "args": ["-thumbnail", "100x100"]}},
    {"type": "ANALYSE"},
    {"type": "EXTRACT", "values": {"dataToExtract": {"AU_METADATA_RESOLUTION": "/image/properties/exif:ResolutionUnit", "GOT_METADATA_METHOD": "/image/properties/exif:SensingMethod", "AU_METADATA_DATE": "/image/properties/xmp:ModifyDate"}}}
  ],
  "inputs": [
    {"name": "test.jpg", "formatId": "fmt/41"}
  ]
}

`result.json` example
~~~~~~~~~~~~~~~~~~~~~

.. code:: json
{
  "requestId": "4f6ae8d7-cab7-4f8d-b5e1-d5c0a1ea5793",
  "id": "1479591e-d325-456f-8409-697f3a757bf7",
  "outputs": {
    "test.jpg": [
      {
        "input": {"name": "test.jpg", "formatId": "fmt/41"},
        "outputName": "GENERATE-test.jpg.GIF",
        "status": "OK",
        "action": "GENERATE"
      },
      {
        "input": {"name": "test.jpg", "formatId": "fmt/41"},
        "status": "OK",
        "analyseResult": "VALID_ALL",
        "action": "ANALYSE"
      },
      {
        "input": {"name": "test.jpg", "formatId": "fmt/41"},
        "outputName": "EXTRACT-test.jpg.json",
        "status": "OK",
        "action": "EXTRACT"
      }
    ]
  }
}