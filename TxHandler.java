import java.util.ArrayList;


public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public UTXOPool utxoPool;
    public ArrayList<Transaction> accTxs = new ArrayList<Transaction>();
    public ArrayList<Transaction> rejTxs = new ArrayList<Transaction>();
    
    public TxHandler(UTXOPool uPool) {
        this.utxoPool = new UTXOPool(uPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        boolean inThePool = false;
        boolean sigIsValid = false;
        double sumOfOutputs = 0;
        double sumOfInputs = 0;
        
        
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        ArrayList<UTXO> utxos = utxoPool.getAllUTXO();
        int numInputs = tx.numInputs();
        
        for (Transaction.Output o : outputs) {
            if (o.value < 0) {
                return false; //negative output
            } else {
                sumOfOutputs += o.value;
            }
        }
        
        
        for (int i = 0; i < numInputs; i++) {
        	UTXO iutxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex); 
        	for (int j = 0; j < numInputs; j++ ) {
        		UTXO jutxo = new UTXO(tx.getInput(j).prevTxHash, tx.getInput(j).outputIndex); 
        		if (iutxo.equals(jutxo) && i != j) {
        			return false; //double-spend
        		}
        	}
            UTXO utxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);        
            for (UTXO ut : utxos) {
                if (ut.equals(utxo)) {
                    inThePool = true;
                    Transaction.Output op = utxoPool.getTxOutput(ut);
                    sumOfInputs += op.value;
                    sigIsValid = Crypto.verifySignature(
                            op.address, tx.getRawDataToSign(i), tx.getInput(i).signature);
                    if (!sigIsValid) {
                        return false; //invalid signature;
                    }
                }      
            }
            if (!inThePool) {
                return false; // claimed utxo not in the pool
            } else {
            	inThePool = false;
            }           
        }
        if (sumOfInputs < sumOfOutputs) {
            return false; // sum of inputs is smaller than sum of outputs
        }
        return true; // all conditions are met
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        //ArrayList<Transaction> rejectedTxs = new ArrayList<Transaction>();
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (!isValidTx(tx)) {
            	rejTxs.remove(tx);
            	rejTxs.add(tx);
                
            } else {
            	acceptedTxs.add(tx);
                ArrayList<UTXO> utxos = utxoPool.getAllUTXO();
                int numInputs = tx.numInputs();
                int numoutputs = tx.numOutputs();
                
                for (int i = 0; i < numInputs; i++) {
                    UTXO utxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);        
                    for (UTXO ut : utxos) {
                        if (ut.equals(utxo)) {
                            utxoPool.removeUTXO(ut);
                        }       
                    }
                }
                
                for (int i = 0; i < numoutputs; i++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);  
                    utxoPool.addUTXO(utxo, tx.getOutput(i));
                }
                
            }
        }
        Transaction[] accTxsArray = new Transaction[ accTxs.size() ];
        accTxs.toArray( accTxsArray );
        if(acceptedTxs.size() == 0) {
        	accTxs = new ArrayList<Transaction>();
        	rejTxs = new ArrayList<Transaction>();
            return accTxsArray;
        }else {
            Transaction[] acceptedTxsArray = new Transaction[ acceptedTxs.size() ];
            acceptedTxs.toArray( acceptedTxsArray );
            for (Transaction tx : acceptedTxsArray) {
                rejTxs.remove(tx);
                accTxs.add(tx); 
            }
            Transaction[] rejTxsArray = new Transaction[ rejTxs.size() ];
            rejTxs.toArray( rejTxsArray );
            return handleTxs(rejTxsArray);
        }
        //return accTxsArray;
    }
}
