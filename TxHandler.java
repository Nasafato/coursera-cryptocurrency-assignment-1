import java.util.HashMap;

import Transaction.Input;

public class TxHandler {

    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        pool = UTXOPool(utxoPool);
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
        double inputSum = 0;
        double outputSum = 0;
        HashMap<UTXO, Integer> utxoFrequency = new HashMap<UTXO, Integer>();

        // go through all outputs claimed by the inputs in the transaction
        // and check that they're in the current UTXO pool and signatures valid
        // Also check for no duplicate UTXO claims
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            // get output claimed by input
            Transaction.Input input = inputs.get(i);
            UTXO utxo = UTXO(input.prevTxHash, input.outputIndex);
            if (!pool.contains(utxo)) {
                return false;
            }

            // make sure this utxo hasn't been claimed twice
            int timesFoundAlready = utxoFrequency.put(utxo, utxoFrequency.getOrDefault(utxo, 0) + 1);
            if (timesFoundAlready >= 1) {
                return false;
            }

            // Get Output from pool and verify signaturee
            Transaction.Output claimedOutput = pool.getTxOutput(utxo);
            boolean verified = Crypto.verifySignature(claimedOutput.address, tx.getRawDataToSign(i), signature);
            if (!verified) {
                return false;
            }

            inputSum += claimedOutput.value;
        }
        // sum of input values >= sum of output values
        // We already have calculated the inputSum, now to calculate outputSum
        // and verify all output values are non-negative
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            Transaction.Output output = outputs.get(i);
            if (output.value < 0) {
                return false;
            }
            outputSum += output.value;
        }

        if (inputSum < outputSum) {
            return false;
        }

        return true;
    }

    // make sure all claimed outputs are not double spent due to this transaction
    private boolean isMutuallyValid(Transaction tx, HashMap<UTXO, Boolean> map) {
        for (int j = 0; j < inputs.size(); j++) {
            Transaction.Input input = inputs.get(j);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (map.containsKey(utxo)) {
                return false;
            }
            map.put(utxo, true);
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        HashMap<UTXO, Boolean> map = new HashMap<UTXO, Boolean>();
        ArrayList<Transaction> acceptedTransactions = new ArrayList<Transaction>();
        for (int i = 0; i < possibleTxs.length; i++) {
            Transaction tx = possibleTxs[i];
            if (!isValidTx(tx)) continue;
            if (!isMutuallyValid(tx, map)) continue;

            acceptedTransactions.add(tx);
        }

        return acceptedTransactions.toArray(new Transaction[acceptedTransactions.size()]);
    }
}
