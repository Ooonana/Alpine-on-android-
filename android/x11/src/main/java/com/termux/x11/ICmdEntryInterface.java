package com.termux.x11;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;

public interface ICmdEntryInterface extends IInterface {
    ParcelFileDescriptor getXConnection() throws RemoteException;

    ParcelFileDescriptor getLogcatOutput() throws RemoteException;

    abstract class Stub extends Binder implements ICmdEntryInterface {
        private static final String DESCRIPTOR = "com.termux.x11.ICmdEntryInterface";
        private static final int TRANSACTION_getXConnection = IBinder.FIRST_CALL_TRANSACTION;
        private static final int TRANSACTION_getLogcatOutput = IBinder.FIRST_CALL_TRANSACTION + 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICmdEntryInterface asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof ICmdEntryInterface) return (ICmdEntryInterface) iin;
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            }

            switch (code) {
                case TRANSACTION_getXConnection: {
                    data.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor result = getXConnection();
                    reply.writeNoException();
                    writeParcelable(reply, result);
                    return true;
                }
                case TRANSACTION_getLogcatOutput: {
                    data.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor result = getLogcatOutput();
                    reply.writeNoException();
                    writeParcelable(reply, result);
                    return true;
                }
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static void writeParcelable(Parcel parcel, ParcelFileDescriptor value) {
            if (value == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                value.writeToParcel(parcel, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
            }
        }

        private static class Proxy implements ICmdEntryInterface {
            private final IBinder remote;

            Proxy(IBinder remote) {
                this.remote = remote;
            }

            @Override
            public IBinder asBinder() {
                return remote;
            }

            @Override
            public ParcelFileDescriptor getXConnection() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_getXConnection, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(reply) : null;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public ParcelFileDescriptor getLogcatOutput() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_getLogcatOutput, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(reply) : null;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }
    }
}
