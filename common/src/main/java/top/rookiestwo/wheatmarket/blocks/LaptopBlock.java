package top.rookiestwo.wheatmarket.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class LaptopBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING= BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<LaptopBlock> CODEC = BlockBehaviour.simpleCodec(LaptopBlock::new);

    public LaptopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        VoxelShape Cover=rotatedVoxel(0.8f, 1.2f, 10.5f, 15.2f, 10.8f, 11.7f, dir);
        VoxelShape Bottom=rotatedVoxel(0.8f, 0.0f, 1.5f, 15.2f, 1.2f, 11.1f, dir);
        return Shapes.or(Cover, Bottom);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    private VoxelShape rotationVoxel(Direction dir){
        VoxelShape Cover;
        VoxelShape Bottom;
        switch (dir){
            case EAST -> {
                Cover = Block.box(4.3f,1.2f,0.8f,5.5f,10.8f,15.2f);
                Bottom = Block.box(4.9f,0.0f,0.8f,14.5f,1.2f,15.2f);
            }
            default -> {
                Cover=Block.box(0.8f, 1.2f, 10.5f, 15.2f, 10.8f, 11.7f);
                Bottom=Block.box(0.8f, 0.0f, 1.5f, 15.2f, 1.2f, 11.1f);
            }
        }
        return Shapes.or(Cover,Bottom);
    }

    private VoxelShape rotatedVoxel(double x1,double y1,double z1,double x2,double y2,double z2,Direction direction) {
        double newX1=0,newZ1=0,newX2=0,newZ2=0;
        switch (direction) {
            case EAST -> {
                newX1 = (double) 16.0F - z1;
                newZ1 = x1;
                newX2 = (double) 16.0F - z2;
                newZ2 = x2;
            }
            case SOUTH -> {
                newX1 = (double) 16.0F - x1;
                newZ1 = (double) 16.0F - z1;
                newX2 = (double) 16.0F - x2;
                newZ2 = (double) 16.0F - z2;
            }
            case WEST -> {
                newX1 = z1;
                newZ1 = (double) 16.0F - x1;
                newX2 = z2;
                newZ2 = (double) 16.0F - x2;
            }
            default -> {
                newX1 = x1;
                newZ1 = z1;
                newX2 = x2;
                newZ2 = z2;
            }
        }
        /*if(newX1 > newX2 || newZ1 > newZ2 ){
            swap(newX1,newX2);
            swap(y1,y2);
            swap(newZ1,newZ2);
        }*/
        if(newX1 > newX2){
            double temp=newX1;
            newX1=newX2;
            newX2=temp;
        }
        if(newZ1 > newZ2){
            double temp=newZ1;
            newZ1=newZ2;
            newZ2=temp;
        }
        return Block.box(newX1, y1, newZ1, newX2, y2, newZ2);
    }
}
