package services;

import beans.request.TransferRequestBean;
import com.google.inject.ImplementedBy;
import services.impl.TransferServiceImpl;

@ImplementedBy(TransferServiceImpl.class)
public interface TransferService {
    boolean transfer(TransferRequestBean transferRequestBean);
}
