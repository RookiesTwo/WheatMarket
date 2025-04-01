package top.rookiestwo.wheatmarket.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;
import top.rookiestwo.wheatmarket.Wheatmarket;

public class LaptopBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING= BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<LaptopBlock> CODEC = BlockBehaviour.simpleCodec(LaptopBlock::new);
    public static final SoundEvent LAPTOP_OPEN = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Wheatmarket.MOD_ID,"laptop_open"));
    public static final SoundEvent LAPTOP_CLOSE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Wheatmarket.MOD_ID,"laptop_close"));

    public LaptopBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if(blockState.getValue(OPEN)){
            if(!player.isShiftKeyDown()){
                togglePower(blockState, level, blockPos, player);
                return InteractionResult.PASS;
            }
        }
        this.toggleOpen(blockState, level, blockPos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING,OPEN,POWERED,WATERLOGGED);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        VoxelShape Cover=null;
        if(state.getValue(OPEN)){
            Cover=rotatedVoxel(0.8f, 1.2f, 10.5f, 15.2f, 10.8f, 11.7f, dir);
        }
        else{
            Cover=rotatedVoxel(0.8, 1.2, 1.5, 15.2, 2.4, 11.1, dir);
        }
        VoxelShape Bottom=rotatedVoxel(0.8f, 0.0f, 1.5f, 15.2f, 1.2f, 11.1f, dir);
        return Shapes.or(Cover, Bottom);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return super.getStateForPlacement(ctx)
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    private void toggleOpen(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            BlockState newState = state.cycle(OPEN);
            if(!newState.getValue(OPEN)&&newState.getValue(POWERED)){
                newState=newState.cycle(POWERED);
            }
            level.setBlock(pos, newState, 3);
        }
        if(state.getValue(OPEN)){
            level.playSound(player, pos, LAPTOP_OPEN, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        }
        else{
            level.playSound(player, pos, LAPTOP_CLOSE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        }
    }

    private void togglePower(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            BlockState newState = state.cycle(POWERED);
            level.setBlock(pos, newState, 3);
        }
    }

    //方块碰撞体积，在水平方向的旋转
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
