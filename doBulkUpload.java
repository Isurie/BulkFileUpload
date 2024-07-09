public ResponseEntity<ResponseDefault> doBulkUpload(Bulk bulk, String token) {
    PendingFundTransferUploadable pendingFundTransferUploadable = new PendingFundTransferUploadable();
    String deliminator=(System.getProperty("os.name").startsWith("Windows"))?"\\":"/";
    if(!folderPath.endsWith(deliminator)) {
        folderPath = folderPath + deliminator;
        log.info("Folder path: " + folderPath);
    }
    String bulkId = dbSequenceManager.getNextBigInteger("SEQ_FND_BULK").toString();
    log.info("%%%%%%%%%%%% fund tran type %%%%%%%%%" + bulk.getFundTrxnType());
    String fileName = folderPath + bulkId + "_" + bulk.getBulkName() + "_" + bulk.getFundTrxnType() + "_" +".csv";
    BufferedWriter writer = null;
    try {
        writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(bulk.getBulk());
    } catch (IOException e) {
        log.error(e.getMessage());
        throw new RuntimeException(e);
    } finally {
        Optional.ofNullable(writer).ifPresent(t -> {
            try {
                t.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    try {
        File file = new File(fileName);
        String hashString = utilityService.getHashedString(file);
        pendingFundTransferUploadable = dataModelMapper.apiToDB(bulk, pendingFundTransferUploadable);
        pendingFundTransferUploadable.setID(new BigInteger(bulkId));
        pendingFundTransferUploadable.setFile_path(fileName);
        pendingFundTransferUploadable.setBulk_name(bulk.getBulkName());
        pendingFundTransferUploadable.setTransactionNumber(new BigInteger(bulk.getTransactionNumber()));
        pendingFundTransferUploadable.setFrm_acc(bulk.getAccountNumber());
        pendingFundTransferUploadable.setBulkTotal(bulk.getBulkTotal());
        pendingFundTransferUploadable.fillMandatory(securityService.getUserName());
        pendingFundTransferUploadable.setCo_id(getCoId());
        pendingFundTransferUploadable.setHash_val(hashString);
        pendingFundTransferUploadable.setChk(bulk.getFundTrxnType());
        BigInteger authTrxnId = dbSequenceManager.getNextBigInteger("SEQ_AUTH_TRXN");
        bulkUploadService.bulkUploadFundTransfers(pendingFundTransferUploadable, authTrxnId,token);
    } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
    }
    return ResponseCreator.successfulResponse(new ResponseDefault(), "01");
}
