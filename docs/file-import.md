# File import

The question library has one File Import button for .txt, .pdf and .docx only.
JSON and legacy .doc imports are not supported.

All formats use the same extraction -> review/edit -> selected question save workflow.
Original files remain in the browser. Only reviewed question data is sent to the API.

- TXT: UTF-8 (including BOM), with EUC-KR fallback.
- PDF: PDF.js text extraction; scanned/image PDFs require OCR and are not supported.
- DOCX: Word document paragraph text, with tabs and line breaks. Images, layout and equations are not interpreted.
- Limits: 15 MB per file; PDF 80 pages; extracted text 500,000 characters; DOCX main XML 5 MB; one batch 200 questions.
- Format rules: numbered questions such as 01., numbered choices such as 1) or circled digits, answer/explanation labels, optional type headings and model-answer markers in Korean.
- TXT and DOCX show Document Body instead of an invented source page number.
- Questions without an identifiable answer require correction before saving.

Existing questions are retained. Batch saving uses the authenticated account and skips matching questions.
Run frontend npm test to check file restrictions, text parsing and invalid-file handling.
