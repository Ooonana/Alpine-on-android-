package com.termux.x11;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRemoteCmdImterface extends IInterface {
    void exit(int code, String output) throws RemoteException;

    abstract class Stub extends Binder implements IRemoteCmdImterface {
        private static final String DESCRIPTOR = "com.termux.x11.IRemoteCmdImterface";
        private static final int TRANSACTION_exit = IBinder.FIRST_CALL_TRANSACTION;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteCmdImterface asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IRemoteCmdImterface) return (IRemoteCmdImterface) iin;
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

            if (code == TRANSACTION_exit) {
                data.enforceInterface(DESCRIPTOR);
                exit(data.readInt(), data.readString());
                reply.writeNoException();
                return true;
            }

            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IRemoteCmdImterface {
            private final IBinder remote;

            Proxy(IBinder remote) {
                this.remote = remote;
            }

            @Override
            public IBinder asBinder() {
                return remote;
            }

            @Override
            public void exit(int code, String output) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(code);
                    data.writeString(output);
                    remote.transact(TRANSACTION_exit, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }
    }
}
