package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.*;
import at.dms.kjc.backendSupport.*;
import java.io.File;
import at.dms.kjc.JBlock;
import java.util.HashMap;

public class ProcessFileReader {
    
    protected FilterSliceNode filterNode;
    protected SchedulingPhase phase;
    protected TileraBackEndFactory factory;
    protected TileCodeStore codeStore;
    protected FileInputContent fileInput;
    protected static HashMap<FilterSliceNode, Tile> allocatingTiles;
    protected Tile allocatingTile;
    
    static {
        allocatingTiles = new HashMap<FilterSliceNode, Tile>();
    }
    
    public ProcessFileReader (FilterSliceNode filter, SchedulingPhase phase, TileraBackEndFactory factory) {
        this.filterNode = filter;
        this.fileInput = (FileInputContent)filter.getFilter();
        this.phase = phase;
        this.factory = factory;
        this.allocatingTile = nextAllocatingTile();
        codeStore = allocatingTile.getComputeCode();
    }
     
    public void processFileReader() {
        if (phase == SchedulingPhase.INIT) {
            long fileSize = getFileSizeBytes();
            JBlock block = new JBlock();
            //create the heap with shared and uncacheable attributes
            codeStore.appendTxtToGlobal("ilibHeap fileReadHeap;\n");
            block.addStatement(Util.toStmt("int flags = ILIB_MEM_SHARED | ILIB_MEM_UNCACHEABLE"));
            block.addStatement(Util.toStmt("ilib_mem_create_heap(flags, &fileReadHeap)"));
            
            codeStore.appendTxtToGlobal(fileInput.getType() + "*fileReadBuffer;\n");
            codeStore.appendTxtToGlobal("int fileReadIndex = 0;\n");
            //malloc enough from the heap to store the entire file
            block.addStatement(Util.toStmt("fileReadBuffer = (int*)ilib_mem_malloc_heap(fileReadHeap, " + 
                    fileSize + ")"));
            
            //open the file read the file into the buffer on the heap
            codeStore.appendTxtToGlobal("FILE *INPUT;\n");
            block.addStatement(Util.toStmt("INPUT = fopen(\"" + fileInput.getFileName() + "\", \"r\")"));
            block.addStatement(Util.toStmt("fread(fileReadBuffer, 1, " + fileSize + ", INPUT)"));
            block.addStatement(Util.toStmt("fclose(INPUT)"));
            
            //now broadcast the address to all other tiles
            block.addStatement(Util.toStmt("ilibStatus ignore"));
            block.addStatement(Util.toStmt("ilib_msg_broadcast(ILIB_GROUP_SIBLINGS, " +
                    TileraBackend.chip.translateTileNumber(codeStore.getParent().getTileNumber()) + ", " +
                    "&fileReadBuffer, sizeof(int *), &ignore)"));
                                
            //generate the receive msg broadcast on the other tiles and the declarations for the buffer/index
            for (Tile other : TileraBackend.chip.getAbstractTiles()) {
                if (codeStore.getParent() == other) 
                    continue;
                
                other.getComputeCode().appendTxtToGlobal(fileInput.getType() + "*fileReadBuffer;\n");
                other.getComputeCode().appendTxtToGlobal("int fileReadIndex = 0;\n");
                               
                JBlock jb = new JBlock();
                jb.addStatement(Util.toStmt("ilibStatus ignore"));
                jb.addStatement(Util.toStmt(
                        "ilib_msg_broadcast(ILIB_GROUP_SIBLINGS, " +
                        TileraBackend.chip.translateTileNumber(codeStore.getParent().getTileNumber()) + ", " +
                    "&fileReadBuffer, sizeof(int *), &ignore)"));
                other.getComputeCode().addStatementFirstToBufferInit(jb);
            }
            
            codeStore.addStatementFirstToBufferInit(block);
        }
    }
    
    /**
     * @return The tile we should allocate this file reader on.  Remember that 
     * the file reader is allocated to off-chip memory.  We just cycle through the tiles
     * if there is more than one file reader, one reader per tile.
     */
    private Tile nextAllocatingTile() {
        for (Tile tile : TileraBackend.chip.getAbstractTiles()) {
            if (!allocatingTiles.containsValue(tile)) {
                allocatingTiles.put(filterNode, tile);
                return tile;
            }
        }
        assert false : "Too many file readers for this chip (one per tile)!";
        return null;
    }
    
    private long getFileSizeBytes() {
        long size = 0;
        
        try {
            File inputFile = new File(fileInput.getFileName());
            size = inputFile.length();
            System.out.println("Input file " + fileInput.getFileName() + " has size " + size);
        } catch (Exception e) {
            System.err.println("Error opening input file: " + fileInput.getFileName());
            System.exit(1);
        }
        return size;
    }
    
}
